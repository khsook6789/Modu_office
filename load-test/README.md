# Modu Office Load Testing (k6)

현업 6단계 부하 테스트 파이프라인. Baseline → Load → Stress → Spike → Soak → Recovery 순서로 시스템 한계를 정량 측정한다.

---

## 🚀 시작하기

### 1. k6 설치

```bash
# Windows
winget install gnu.k6

# macOS
brew install k6
```

### 2. 환경 설정

```bash
# 컨테이너 + 볼륨 완전 삭제 후 클린 빌드 (항상 이 방법 권장)
docker compose -f docker-compose.prod.yml down -v
docker compose -f docker-compose.prod.yml up -d --build
```

> **배포 모드 vs 부하 테스트 모드 전환**: `.env` 파일만 교체하면 된다. 자세한 내용은 아래 [모드 전환 가이드](#-모드-전환-가이드-배포--부하-테스트)를 참고.

### 3. 부하 테스트 실행

**Docker Desktop과 IntelliJ를 켠 후 PowerShell에서 실행:**

```powershell
# 실행 (모든 것이 자동화됨)
& "C:\Program Files\Git\bin\bash.exe" load-test/run-tests.sh
```

스크립트가 자동으로 처리하는 것:
- 컨테이너 미실행 시 `up --build` 자동 실행
- 시나리오마다 backend 재시작 → 시딩 완료 확인 → k6 실행 → 메트릭 수집
- 결과를 `reports-N/` 폴더에 시나리오별로 저장 (N은 회차 자동 증가)
- 완료 후 PASS/FAIL 요약 출력

> 전체 소요시간 약 1시간 (soak 30분 포함). 실행 후 자리를 비워도 된다.

---

## 🗄️ 시딩 데이터 상세

부하 테스트 전 `DataInitializer`가 자동으로 다음 데이터를 생성한다.
(`DATA_SEEDING_ENABLED=true`, `DATA_CLEAN_BEFORE_SEEDING=true` 설정 시)

### 계정 및 사용자

| 항목 | 수량 | 상세 |
|------|------|------|
| USER 계정 | 5,000명 | `loadtest-user-0001@test.com` ~ `loadtest-user-5000@test.com` |
| MANAGER 계정 | 500명 | `loadtest-manager-001@test.com` ~ `loadtest-manager-500@test.com` |
| 공통 비밀번호 | - | `Test1234!` |
| MANAGER 승인 상태 | - | 전원 `APPROVED` (로그인 즉시 가능) |

> **VU : 계정 = 1 : 1 원칙** 준수. Spike 테스트 최대 5,000 VU 기준으로 USER 5,000명 설계.

### 지점 및 회의실

| 항목 | 수량 | 상세 |
|------|------|------|
| 지점 (Office) | 100개 | 서울(강남/서초/마포/종로/영등포/송파/중구), 부산, 대전, 인천, 대구, 광주, 수원, 성남, 제주, 울산 — **20개 도시 분산** |
| 영업일 패턴 | 3종 | Mon-Fri(40%) / Mon-Sat(40%) / Mon-Sun(20%) — 다양한 쿼리 플랜 유도 |
| 회의실 (Room) | 500개 | 지점당 5개 |
| 카테고리 | 8종 | 회의실·세미나실·스터디룸·프레젠테이션룸·소회의실·대회의실·미팅룸·워크샵룸 |
| 가격대 | 8단계 | 8,000원 ~ 50,000원 (8,000 / 12,000 / 15,000 / 20,000 / 25,000 / 30,000 / 40,000 / 50,000) |
| 층수 | 1~10층 | 순환 배치 |
| 상태 | AVAILABLE / INACTIVE | **95% AVAILABLE, 5% INACTIVE** (약 25개) — 필터 쿼리 다양성 확보 |
| 편의시설 | 15종 | Wi-Fi·Projector·Whiteboard·Monitor 등. 회의실당 **2~4개 랜덤** 부착 |

### 예약

| 항목 | 수량 | 상세 |
|------|------|------|
| 총 예약 | ~12,500건 | 회의실 500개 × 25건 |
| 시간 분포 | 30일 분산 | 영업시간 **09:00 ~ 17:00** 내 배치 |
| 시간대 집중 구간 | 의도적 배치 | 회의실당 마지막 5건은 09~11시대 근접 배치 → **예약 겹침 감지 로직(Optimistic Lock) 실제 작동** 보장 |
| 상태 분포 | 4종 | CONFIRMED 60% / PENDING_APPROVAL 20% / PENDING_PAYMENT 10% / CANCELED 10% |
| 리뷰 | ~5,000건 | CONFIRMED 예약 기준. 별점 1~5 랜덤 |
| 환불 정책 | 300건 | 지점당 3단계 (7일전 100% / 3일전 50% / 1일전 0%) |

---

## 🧪 시나리오 상세

### 공통 트래픽 구성 (전 시나리오 동일)

모든 시나리오는 실제 서비스 사용 패턴을 반영한 **3개 페르소나 트래픽**으로 구성된다.

```
70% USER 여정:
  로그인 → 지점 검색 → 주변 회의실 검색(도시 4곳 랜덤) → 회의실 상세 →
  예약 생성(201 성공 or 409 충돌 모두 정상) → 내 예약 목록 → 20% 확률 취소

20% MANAGER 여정:
  로그인 → 내 지점 목록 → 승인 대기 예약 조회 → 실제 ID로 예약 확정 → 통계 조회

10% BROWSE 여정:
  비인증 지점 목록 → 회의실 검색 (window-shopper 시뮬레이션)
```

Think time: 각 액션 사이 1~5초 랜덤 (`Math.random()` 기반) — 인위적 동기화 방지

---

### 01 - Baseline (기준선)

```
VU: 1명 / 시간: 60초
Threshold: p(95) < 500ms, server_errors < 1%
```

**목적**: 단일 사용자 조건에서 각 API의 순수 응답 시간을 측정한다. 이 수치가 이후 모든 시나리오의 성능 기준선이 된다. 여기서 이미 느리다면 코드 레벨 문제이다.

**이 테스트로 확인하는 것**:
- 각 API의 Cold-start 없는 최소 응답 시간
- 기능 동작 여부 (check() 통과율)
- 서버 에러 0%인지 확인

---

### 02 - Load (정상 부하)

```
VU: 100명 (고정) / 시간: 5분
Threshold: p(95) < 500ms, server_errors < 5%
```

**목적**: "일반적인 피크 타임"을 시뮬레이션한다. 회의실 예약 서비스 특성상 오전 출근 직후 100명이 동시 접속하는 상황과 유사하다.

**이 테스트로 확인하는 것**:
- 정상 부하에서 SLO(p95 < 500ms) 달성 여부
- Soak과 비교하기 위한 안정 상태 레이턴시 기록
- DB 커넥션 풀이 정상 범위에서 작동하는지

---

### 03 - Stress (스트레스 / 한계 탐색)

```
VU: 50 → 200 → 500 → 1000 → 2000 (단계적 증가) / 시간: 15분
Threshold: p(95) < 1000ms, server_errors < 10%
```

**목적**: Breaking Point를 찾는다. 어느 VU 구간부터 응답 시간이 급격히 증가하는지, 그 지점이 어디인지 파악한다. 최적화 전후 비교의 핵심 시나리오.

**이 테스트로 확인하는 것**:
- 성능 저하 시작 VU 수 (병목 시작점)
- Threshold FAIL 시 어느 단계에서 발생했는지
- `collect-metrics.sh` 결과와 대조하여 병목 원인 특정 (HikariCP 포화? Thread 포화? GC 폭발?)

---

### 04 - Spike (스파이크)

```
VU: 100 → 5000 (10초 폭발) → 5000 유지 1분 → 100으로 감소
Threshold: p(95) < 10000ms, server_errors < 20%
```

**목적**: 자격증 시험 신청처럼 순간적으로 폭발적인 트래픽이 유입될 때 서버가 죽지 않고 생존하는지 검증한다. 레이턴시 악화는 허용하되, 5xx 에러 20% 초과는 장애로 판정한다.

**이 테스트로 확인하는 것**:
- 극한 트래픽에서 서버 생존 여부
- 동시 예약 폭발 시 Optimistic Lock(409)이 안정적으로 작동하는지
- 인터럽트된 iteration 수 (서버 수용 한계 초과 측정)

---

### 05 - Soak (장기 내구)

```
VU: 200명 (고정) / 시간: 30분
Threshold: p(95) < 1000ms, server_errors < 5%
```

**목적**: 장시간 운영에서 발생하는 **점진적 열화**를 감지한다. 처음 5분과 마지막 5분의 레이턴시 차이가 크면 메모리 누수 또는 커넥션 풀 고갈이다.

**이 테스트로 확인하는 것**:
- 30분 후에도 p95가 초기와 동일한지
- JVM 힙이 선형으로 증가하는지 (누수 징후)
- HikariCP idle 커넥션이 점점 줄어드는지 (누수 징후)

---

### 06 - Recovery (회복)

```
VU: 200 → 3000 (과부하 유도) → 3000 유지 2분 → 200으로 감소
Threshold: p(95) < 500ms, server_errors < 5%
```

**목적**: 과부하 이후 시스템이 **baseline 수준으로 복귀**하는지 검증한다. threshold가 Load(200 VU)와 동일한 기준인 이유는, 부하 해소 후 반드시 정상 상태로 돌아와야 하기 때문이다.

**이 테스트로 확인하는 것**:
- 과부하 후 응답 시간 정상 복귀 여부
- 커넥션 누수 없이 풀이 idle 상태로 회복되는지
- 완료되지 못한 iteration(인터럽트) 수

---

## 📊 결과 해석 가이드

### Threshold 기준값 근거

| 시나리오 | p(95) 임계치 | server_errors 임계치 | 근거 |
|---------|-------------|---------------------|------|
| Baseline/Load | 500ms | 1~5% | 사용자 여정 5단계(로그인~예약목록) 전체가 3초 이내 완결 목표. 단계당 최대 600ms 허용. Google RAIL 모델 기준 |
| Stress | 1,000ms | 10% | 설계 용량 초과 시 graceful degradation 허용. 느려도 죽지는 않아야 함 |
| Spike | 10,000ms | 20% | 극한 트래픽에서 생존 자체가 목표. 단 5xx 20% 초과 시 장애로 판정 |
| Soak | 1,000ms | 5% | 장기 운영 중 성능 저하 없음을 증명. Load와 동일한 VU이므로 p95도 유사해야 함 |
| Recovery | 500ms | 5% | 과부하 해소 후 반드시 baseline 수준으로 복귀했음을 증명 |

### 커스텀 메트릭 분류

k6 내장 `http_req_failed`는 4xx + 5xx 전체를 실패로 집계한다. 원인 추적을 위해 다음 커스텀 메트릭으로 세분화한다:

| 메트릭 | HTTP Status | 의미 |
|--------|-------------|------|
| `server_errors` | 5xx | 실제 서버 장애 (DB 다운, OOM, 미처리 예외 등) |
| `business_rejects` | 400, 409 | 비즈니스 로직에 의한 정상 거절 (시간 충돌, 유효성 오류). 동시성 제어가 작동 중임을 의미 |
| `auth_failures` | 401, 403 | 인증/인가 실패 (토큰 만료, 권한 없음). BROWSE 여정의 비인증 접근 포함 |
| `not_found` | 404 | 리소스 미존재. INACTIVE 회의실 조회 등 시나리오상 예상되는 케이스 |

### `http_req_failed` ~26% 해석

```
http_req_failed ~26% 내역:
├── business_rejects ~15%  ← 409(예약 충돌) + 400(유효성 오류). 동시성 제어 정상 작동 증거.
├── auth_failures    ~3%   ← 401/403. BROWSE 여정(비인증) + 토큰 만료 케이스.
├── not_found        ~8%   ← 404. INACTIVE Room 조회 등 예상 범위 내 미스.
└── server_errors    ~0%   ← 5xx. 실제 서버 에러. 이 수치가 시스템 건강 지표.
```

**핵심**: `http_req_failed` 26%가 아닌 `server_errors` ≈ 0%가 시스템이 건강하다는 지표다.

---

## 📈 인프라 메트릭 수집 (병목 원인 추적)

k6 결과만으로는 "왜 느린가"를 진단할 수 없다. `collect-metrics.sh`를 부하 테스트와 병행하여 실행한다.

### collect-metrics.sh 동작 원리

5초마다 두 가지 소스에서 데이터를 수집하여 CSV로 저장한다:

**1. `docker stats` (컨테이너 리소스)**
```
docker-stats.csv
  timestamp, container, cpu_percent, mem_usage, mem_limit, net_io_in, net_io_out
```
→ CPU가 100%에 붙으면 **Thread 포화**, 메모리가 선형 증가하면 **메모리 누수**

**2. Spring Actuator metrics (JVM + DB)**
```
actuator-metrics.csv
  timestamp, hikari_active, hikari_idle, hikari_pending, jvm_heap_used_mb, jvm_heap_max_mb, jvm_gc_pause_count
```
→ `hikari_active`가 max(50)에 붙고 `hikari_pending` > 0이면 **커넥션 풀 포화**
→ `jvm_heap_used_mb`가 지속 증가하면 **메모리 누수**
→ `jvm_gc_pause_count`가 폭발적으로 증가하면 **GC 압박**

### 사용법

```bash
# 별도 터미널에서 부하 테스트 시작 전에 먼저 실행
cd load-test
./collect-metrics.sh reports-n    # Ctrl+C로 종료

# 이후 다른 터미널에서 k6 시나리오 실행
```

### 진단 예시

```
Stress 테스트 결과 p95 = 2.7s (임계치 초과)
→ actuator-metrics.csv 확인
  → hikari_active = 50 (max), hikari_pending = 23  ⟹  DB 커넥션 풀 포화
  → 조치: HikariCP maximum-pool-size 증설 (application-prod.yml)
```

---

## 🔀 모드 전환 가이드 (배포 ↔ 부하 테스트)

`docker-compose.prod.yml` 하나로 두 모드를 운영한다. `.env` 파일의 **4개 변수만** 바꾸면 전환된다. DB 자격증명·시크릿 등 나머지 값은 그대로 유지한다.

| 변수 | 부하 테스트 | 배포 |
|------|------------|------|
| `POSTGRES_DB` | `modu_office_loadtest` | `modu_office` |
| `DATA_SEEDING_ENABLED` | `true` | `false` |
| `DATA_CLEAN_BEFORE_SEEDING` | `true` | `false` |
| `NGINX_CONF` | `./load-test/nginx-loadtest.conf` | `./frontend/nginx.conf` |

`.env` 파일에 두 모드의 값이 주석으로 함께 제공되어 있다. 해당 줄의 주석을 해제/교체하면 된다.

---

## 🛠️ 주의 사항

- **데이터 정합성**: Soak 테스트 중 응답 시간이 느려진다면 `docker-stats.csv`에서 메모리 증가 여부로 누수와 데이터 누적을 구분한다.
- **SSE 제외**: 부하 테스트 안정성을 위해 SSE 구독(`/notifications/subscribe`) 엔드포인트는 시나리오에서 제외됐다. SSE는 long-lived connection을 유지하므로 별도 측정이 필요하다.
- **Clean Up**: 테스트 후 깨끗한 상태로 복구하려면 아래 명령을 사용한다.
  ```bash
  docker compose -f docker-compose.prod.yml down -v
  ```
