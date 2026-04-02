#!/bin/bash
# 부하 테스트 중 인프라 메트릭 수집 스크립트
# 사용법: ./collect-metrics.sh <출력폴더>
# 종료: Ctrl+C (run-tests.sh에서 자동 종료됨)

REPORT_DIR="${1:-reports-tmp}"
INTERVAL=5
BASE_URL="http://localhost:8080"

mkdir -p "$REPORT_DIR"

DOCKER_LOG="$REPORT_DIR/docker-stats.csv"
ACTUATOR_LOG="$REPORT_DIR/actuator-metrics.csv"

# CSV 헤더
echo "timestamp,container,cpu_pct,mem_usage_mib,mem_limit_mib,net_in_mb,net_out_mb" > "$DOCKER_LOG"
echo "timestamp,hikari_active,hikari_idle,hikari_pending,jvm_heap_used_mb,jvm_heap_max_mb,jvm_gc_pause_count" > "$ACTUATOR_LOG"

# docker stats MemUsage/NetIO 파싱: 단위(MiB/GiB/MB/GB/kB/B)를 MiB 또는 MB 단위 숫자로 변환
parse_bytes_to_mib() {
    local raw="$1"
    echo "$raw" | awk '{
        val = $1; unit = $2
        if (unit == "GiB") val = val * 1024
        else if (unit == "kB")  val = val / 1024
        else if (unit == "B")   val = val / 1048576
        # MiB는 그대로
        printf "%.1f", val
    }'
}

parse_net_to_mb() {
    local raw="$1"
    echo "$raw" | awk '{
        val = $1; unit = $2
        if (unit == "GB") val = val * 1024
        else if (unit == "kB") val = val / 1024
        else if (unit == "B")  val = val / 1048576
        # MB는 그대로
        printf "%.2f", val
    }'
}

while true; do
    TS=$(date '+%Y-%m-%d %H:%M:%S')

    # ── Docker stats ────────────────────────────────
    # MemUsage: "1.23GiB / 4GiB", NetIO: "12.3MB / 45.6MB"
    docker stats --no-stream \
        --format "{{.Name}}|{{.CPUPerc}}|{{.MemUsage}}|{{.NetIO}}" 2>/dev/null \
    | while IFS='|' read -r name cpu mem net; do
        # CPU: "12.34%" → "12.34"
        cpu_val="${cpu//%/}"

        # MemUsage: "1.23GiB / 4GiB" → 앞/뒤 분리
        mem_used_raw=$(echo "$mem" | awk -F' / ' '{print $1}' | xargs)
        mem_lim_raw=$(echo "$mem"  | awk -F' / ' '{print $2}' | xargs)
        mem_used=$(parse_bytes_to_mib "$mem_used_raw")
        mem_lim=$(parse_bytes_to_mib "$mem_lim_raw")

        # NetIO: "1.2MB / 3.4MB" → 앞/뒤 분리
        net_in_raw=$(echo "$net"  | awk -F' / ' '{print $1}' | xargs)
        net_out_raw=$(echo "$net" | awk -F' / ' '{print $2}' | xargs)
        net_in=$(parse_net_to_mb "$net_in_raw")
        net_out=$(parse_net_to_mb "$net_out_raw")

        echo "$TS,$name,$cpu_val,$mem_used,$mem_lim,$net_in,$net_out" >> "$DOCKER_LOG"
    done

    # ── Actuator metrics (--max-time으로 hang 방지) ──
    HIKARI_ACTIVE=$(curl -sf --max-time 2 "$BASE_URL/actuator/metrics/hikaricp.connections.active"  2>/dev/null | grep -o '"value":[0-9.]*' | head -1 | cut -d: -f2)
    HIKARI_IDLE=$(  curl -sf --max-time 2 "$BASE_URL/actuator/metrics/hikaricp.connections.idle"     2>/dev/null | grep -o '"value":[0-9.]*' | head -1 | cut -d: -f2)
    HIKARI_PENDING=$(curl -sf --max-time 2 "$BASE_URL/actuator/metrics/hikaricp.connections.pending" 2>/dev/null | grep -o '"value":[0-9.]*' | head -1 | cut -d: -f2)
    JVM_HEAP=$(     curl -sf --max-time 2 "$BASE_URL/actuator/metrics/jvm.memory.used?tag=area:heap" 2>/dev/null | grep -o '"value":[0-9.]*' | head -1 | cut -d: -f2)
    JVM_MAX=$(      curl -sf --max-time 2 "$BASE_URL/actuator/metrics/jvm.memory.max?tag=area:heap"  2>/dev/null | grep -o '"value":[0-9.]*' | head -1 | cut -d: -f2)
    GC_COUNT=$(     curl -sf --max-time 2 "$BASE_URL/actuator/metrics/jvm.gc.pause"                  2>/dev/null | grep -o '"count":[0-9]*'  | head -1 | cut -d: -f2)

    # bytes → MB 변환 (awk BEGIN은 stdin 불필요)
    JVM_HEAP_MB=$(awk "BEGIN{printf \"%.0f\", ${JVM_HEAP:-0}/1048576}")
    JVM_MAX_MB=$( awk "BEGIN{printf \"%.0f\", ${JVM_MAX:-0}/1048576}")

    echo "$TS,${HIKARI_ACTIVE:-0},${HIKARI_IDLE:-0},${HIKARI_PENDING:-0},$JVM_HEAP_MB,$JVM_MAX_MB,${GC_COUNT:-0}" >> "$ACTUATOR_LOG"

    sleep $INTERVAL
done
