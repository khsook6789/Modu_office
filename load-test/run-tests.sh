#!/bin/bash
# 부하 테스트 자동화 실행 스크립트
# 사용법: ./run-tests.sh
# 종료: Ctrl+C (진행 중인 k6 및 메트릭 수집 자동 정리)

set -uo pipefail  # -e 제외: 개별 명령 실패를 직접 처리

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/../docker-compose.prod.yml"
BACKEND_URL="http://localhost:8080"

# ── 회차 번호 자동 결정 ─────────────────────────────
NEXT_N=1
for dir in "$SCRIPT_DIR"/reports-*/; do
    [[ -d "$dir" ]] || continue
    NUM="${dir%/}"
    NUM="${NUM##*reports-}"
    [[ "$NUM" =~ ^[0-9]+$ ]] && (( NUM + 1 > NEXT_N )) && NEXT_N=$(( NUM + 1 ))
done
REPORT_ROOT="$SCRIPT_DIR/reports-$NEXT_N"

# ── 시나리오 목록 ───────────────────────────────────
SCENARIOS=(
    "01-baseline"
    "02-load"
    "03-stress"
    "04-spike"
    "05-soak"
    "06-recovery"
)

# ── 상태 추적 ───────────────────────────────────────
METRICS_PID=""
K6_PID=""
declare -A RESULTS
declare -A DURATIONS
START_TOTAL=$(date +%s)

# ── 정리 함수 (Ctrl+C / 비정상 종료 시 호출) ────────
cleanup() {
    echo ""
    echo ">> 중단 감지: 실행 중인 프로세스 정리 중..."
    [[ -n "$K6_PID" ]] && kill "$K6_PID" 2>/dev/null && echo "   k6 종료 (PID $K6_PID)"
    [[ -n "$METRICS_PID" ]] && kill "$METRICS_PID" 2>/dev/null && echo "   메트릭 수집 종료 (PID $METRICS_PID)"
    echo ">> 정리 완료. 부분 결과: $REPORT_ROOT"
    exit 1
}
trap cleanup INT TERM

# ── 의존성 확인 ─────────────────────────────────────
check_dependencies() {
    local missing=0
    for cmd in k6 docker curl; do
        if ! command -v "$cmd" &>/dev/null; then
            echo "[ERROR] '$cmd' 가 설치되지 않았습니다."
            missing=1
        fi
    done
    [[ $missing -eq 1 ]] && exit 1
}

# ── .env 모드 확인 ──────────────────────────────────
check_env_mode() {
    local env_file="$SCRIPT_DIR/../.env"
    if [[ ! -f "$env_file" ]]; then
        echo "[ERROR] .env 파일이 없습니다: $env_file"
        exit 1
    fi

    local seeding
    seeding=$(grep -E "^DATA_SEEDING_ENABLED=" "$env_file" | cut -d= -f2 | tr -d '[:space:]')
    if [[ "$seeding" != "true" ]]; then
        echo "[ERROR] .env의 DATA_SEEDING_ENABLED=${seeding:-미설정} — 부하 테스트 모드가 아닙니다."
        echo "        .env에서 아래 값으로 변경 후 다시 실행하세요:"
        echo "          POSTGRES_DB=modu_office_loadtest"
        echo "          DATA_SEEDING_ENABLED=true"
        echo "          DATA_CLEAN_BEFORE_SEEDING=true"
        echo "          NGINX_CONF=./load-test/nginx-loadtest.conf"
        exit 1
    fi

    local db
    db=$(grep -E "^POSTGRES_DB=" "$env_file" | cut -d= -f2 | tr -d '[:space:]')
    if [[ "$db" != "modu_office_loadtest" ]]; then
        echo "[WARN] POSTGRES_DB=${db} — 부하 테스트용 DB(modu_office_loadtest)가 아닙니다. 계속하려면 Enter, 중단하려면 Ctrl+C"
        read -r
    fi
}

# ── 컨테이너 이름 동적 조회 ─────────────────────────
get_backend_container() {
    local name
    name=$(docker compose -f "$COMPOSE_FILE" ps -q backend 2>/dev/null | head -1 | xargs -I{} docker inspect --format='{{.Name}}' {} 2>/dev/null | tr -d '/')
    echo "$name"
}

# ── 컨테이너 기동 확인 및 최초 up ───────────────────
ensure_containers_running() {
    echo ">> 컨테이너 상태 확인..."
    local running
    running=$(docker compose -f "$COMPOSE_FILE" ps --status running --quiet 2>/dev/null | wc -l | tr -d '[:space:]')

    if [[ "$running" -lt 3 ]]; then
        echo ">> 컨테이너가 실행 중이 아닙니다. 최초 기동 중 (--build 포함)..."
        docker compose -f "$COMPOSE_FILE" up -d --build
        echo ">> 최초 기동 완료."
    else
        echo "   컨테이너 정상 실행 중 ($running 개)."
    fi
}

# ── backend healthy 대기 ────────────────────────────
wait_for_health() {
    echo ">> backend 헬스체크 대기 중..."
    local retries=0
    until curl -sf "$BACKEND_URL/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; do
        sleep 5
        (( retries++ ))
        if (( retries > 36 )); then  # 3분
            echo "[ERROR] backend 헬스체크 3분 초과. 로그 확인:"
            docker compose -f "$COMPOSE_FILE" logs --tail=20 backend
            exit 1
        fi
    done
    echo "   헬스체크 통과."
}

# ── 시딩 완료 대기 (재시작 이후 로그만 검색) ─────────
wait_for_seeding() {
    local container="$1"
    echo ">> 데이터 시딩 완료 대기 중..."

    # 재시작 직후 컨테이너의 StartedAt 기준으로 이후 로그만 검색
    local started_at
    started_at=$(docker inspect --format='{{.State.StartedAt}}' "$container" 2>/dev/null)
    if [[ -z "$started_at" ]]; then
        echo "[ERROR] 컨테이너 '$container' StartedAt 조회 실패."
        exit 1
    fi

    local retries=0
    until docker logs --since "$started_at" "$container" 2>&1 | grep -q "Data seeding completed"; do
        sleep 3
        (( retries++ ))
        if (( retries > 60 )); then  # 3분
            echo "[ERROR] 시딩 완료 로그 미감지 (3분 초과)."
            echo "        마지막 로그:"
            docker logs --since "$started_at" --tail=10 "$container" 2>&1
            exit 1
        fi
    done
    echo "   시딩 완료 확인."
}

# ── 포맷 헬퍼 ───────────────────────────────────────
format_duration() {
    local secs=$1
    printf "%dm %02ds" $(( secs / 60 )) $(( secs % 60 ))
}

# ════════════════════════════════════════════════════
#  메인 실행
# ════════════════════════════════════════════════════
echo "========================================"
echo "  Modu Office 부하 테스트 자동화"
echo "  결과 폴더: reports-$NEXT_N"
echo "========================================"

check_dependencies
check_env_mode
ensure_containers_running
mkdir -p "$REPORT_ROOT"

for SCENARIO in "${SCENARIOS[@]}"; do
    SCENARIO_DIR="$REPORT_ROOT/$SCENARIO"
    mkdir -p "$SCENARIO_DIR"

    echo ""
    echo "----------------------------------------"
    echo "  [$SCENARIO] 시작"
    echo "----------------------------------------"

    # 1. backend 재시작 → DataInitializer 실행
    echo ">> backend 재시작 (DB 초기화 + 시딩)..."
    docker compose -f "$COMPOSE_FILE" restart backend

    # 재시작 후 컨테이너 이름 재조회 (ID 변경 가능성 대비)
    BACKEND_CONTAINER=$(get_backend_container)
    if [[ -z "$BACKEND_CONTAINER" ]]; then
        echo "[ERROR] backend 컨테이너 이름 조회 실패."
        exit 1
    fi

    wait_for_health
    wait_for_seeding "$BACKEND_CONTAINER"

    # 2. 메트릭 수집 백그라운드 시작
    METRICS_PID=""
    bash "$SCRIPT_DIR/collect-metrics.sh" "$SCENARIO_DIR" &
    METRICS_PID=$!
    echo ">> 메트릭 수집 시작 (PID $METRICS_PID)"

    # 3. k6 실행
    SCENARIO_START=$(date +%s)
    K6_EXIT=0
    K6_PID=""

    k6 run "$SCRIPT_DIR/scenarios/${SCENARIO}.js" \
        > "$SCENARIO_DIR/summary.txt" 2>&1 &
    K6_PID=$!
    echo ">> k6 실행 중 (PID $K6_PID)..."

    wait "$K6_PID" || K6_EXIT=$?
    K6_PID=""

    SCENARIO_END=$(date +%s)
    DURATIONS[$SCENARIO]=$(( SCENARIO_END - SCENARIO_START ))

    # 4. 메트릭 수집 중단
    kill "$METRICS_PID" 2>/dev/null
    wait "$METRICS_PID" 2>/dev/null || true
    METRICS_PID=""

    # 5. 결과 판정
    if [[ $K6_EXIT -eq 0 ]]; then
        RESULTS[$SCENARIO]="PASS"
        echo ">> [$SCENARIO] 완료: PASS ($(format_duration "${DURATIONS[$SCENARIO]}"))"
    else
        RESULTS[$SCENARIO]="FAIL (exit $K6_EXIT)"
        echo ">> [$SCENARIO] 완료: FAIL (exit $K6_EXIT, $(format_duration "${DURATIONS[$SCENARIO]}"))"
    fi
done

# ── 최종 요약 ───────────────────────────────────────
END_TOTAL=$(date +%s)
TOTAL_SECS=$(( END_TOTAL - START_TOTAL ))

echo ""
echo "========================================"
echo "  부하 테스트 완료: reports-$NEXT_N"
echo "========================================"
for SCENARIO in "${SCENARIOS[@]}"; do
    STATUS="${RESULTS[$SCENARIO]:-SKIPPED}"
    DURATION="${DURATIONS[$SCENARIO]:-0}"
    printf "  %-14s [%s]  %s\n" "$SCENARIO" "$STATUS" "$(format_duration "$DURATION")"
done
echo "----------------------------------------"
printf "  총 소요시간: %s\n" "$(format_duration "$TOTAL_SECS")"
echo "========================================"
