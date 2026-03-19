# 모두의 오피스 (Modu_office)

**Modu_office**는 기업 내 회의실과 공용 공간을 예약·관리할 수 있는 **자원 관리 플랫폼**입니다.
이중 예약 방지, 실시간 알림, 결제 연동까지 갖춘 풀스택 예약 시스템입니다.

---

## 핵심 목표 (Core Objectives)

- **서비스 안정성 (Stability)**: `@Version` 기반 낙관적 락(Optimistic Locking)으로 동시 예약 요청 간 데이터 충돌 없이 중복 예약을 차단합니다.
- **성능 최적화 (Performance)**: JPA `@EntityGraph`와 Fetch Join으로 N+1 문제를 해소하고, Caffeine 캐시로 빈번한 조회 응답 속도를 개선했습니다.
- **실시간 응답성 (Responsiveness)**: SSE(Server-Sent Events)를 통해 예약 현황 변화와 알림을 새로고침 없이 즉시 전달합니다.
- **데이터 신뢰성 (Reliability)**: PostgreSQL JSONB를 활용해 예약 변경 이력의 Before/After 스냅샷을 감사 로그로 기록합니다.
- **역할 기반 접근 제어 (RBAC)**: 계정(Account)과 프로필(AppUser)을 분리하고, USER / MANAGER / ADMIN 세 역할로 기능 접근을 제어합니다.

---

## 기술 스택 (Tech Stack)

### Backend

| category          | Technology                    | Description                                                                           |
| :---------------- | :---------------------------- | :------------------------------------------------------------------------------------ |
| **Framework**     | Spring Boot 3.5.10            | RESTful API 및 비즈니스 로직 핵심 프레임워크                                          |
| **Language**      | Java 21                       | 현대적 자바 기능 기반의 안정적인 백엔드 코드                                          |
| **Persistence**   | Spring Data JPA               | 객체 지향적 데이터 접근 및 관계 매핑 관리                                             |
| **Database**      | PostgreSQL 15                 | 고성능 동시성 제어 및 JSONB 타입을 통한 로그 적재                                     |
| **Security**      | Spring Security + JWT         | 무상태(Stateless) 기반의 보안 인증 및 권한 관리                                       |
| **Real-time**     | Server-Sent Events (SSE)      | 예약 현황 및 실시간 알림의 단방향 스트리밍 구현                                       |
| **Validation**    | Jakarta Validation            | DTO 입력값 검증 및 데이터 무결성 보장                                                 |
| **Testing**       | JUnit 5                       | 단위/통합 테스트를 통한 코드 신뢰성 확보                                              |
| **Logging**       | SLF4J                         | 시스템 동작 추적 및 디버깅을 위한 로깅                                                |
| **Cache**         | Caffeine                      | 로컬 인메모리 캐시를 통한 빈번한 조회 성능 최적화                                     |
| **Query**         | QueryDSL                      | 복잡한 동적 조건 검색을 타입 안전하게 처리                                            |
| **Migration**     | Flyway                        | DB 스키마 버전 관리 및 마이그레이션 자동화                                            |
| **Infra**         | Docker / Docker Compose       | 백엔드·DB·프론트엔드 컨테이너 통합 개발 환경 구성                                     |
| **DevOps**        | Spring Boot DevTools          | 핫 리로드 등 개발 생산성 향상 도구                                                    |
| **External API**  | Google Maps API               | 지점 위치 정보 및 거리 기반 검색 서비스 제공                                          |
|                   | Toss Payments                 | 결제 위젯 및 API 연동을 통한 온라인 안전 결제 처리                                    |
| **Auxiliary**     | Lombok                        | 보일러플레이트 코드 제거를 통한 생산성 향상                                           |
| **Build Tool**    | Gradle                        | 프로젝트 빌드 및 의존성 라이프사이클 관리                                             |
| **Documentation** | Spring REST Docs + Swagger UI | 테스트 기반으로 검증된 API 명세를 OpenAPI 3.0 YAML로 자동 생성 후 Swagger UI로 렌더링 |

### Frontend

| category          | Technology     | Description                                     |
| :---------------- | :------------- | :---------------------------------------------- |
| **Framework**     | React 19       | 컴포넌트 기반의 직관적인 사용자 인터페이스 구현 |
| **Environment**   | Vite 7         | 현대적이고 빠른 빌드/HMR 환경 제공              |
| **Language**      | TypeScript     | 엄격한 타입 체크를 통한 코드 안정성 향상        |
| **Styling**       | Vanilla CSS    | 프로젝트 고유의 스타일 테마 및 일관된 UI 구성   |
| **Routing**       | React Router 7 | 싱글 페이지 애플리케이션(SPA) 내비게이션 관리   |
| **Communication** | Axios          | 백엔드 API와의 효율적인 비동기 통신             |

---

## 🚀 시작하기 (Getting Started)

### 1. 환경변수 준비

| 파일 | 용도 | 참고 |
| :--- | :--- | :--- |
| `.env` | Docker DB 컨테이너 | `.env.example` 참고 |
| `backend/src/main/resources/application-local.yml` | 백엔드 실행 | `application-local.example.yml` 참고 |
| `frontend/.env` | 프론트엔드 지도 API | `VITE_GOOGLE_MAPS_API_KEY` 입력 |

### 2. DB 실행 (Docker)

```bash
docker-compose up -d db
```

PostgreSQL이 `localhost:5433`에서 실행됩니다.

### 3. 백엔드 / 프론트엔드 실행

```bash
# 백엔드
./gradlew bootRun   # http://localhost:8080

# 프론트엔드
cd frontend
npm install
npm run dev         # http://localhost:5173
```

### API 문서 (Swagger UI)

서버 실행 후 아래 주소에서 전체 API 명세 및 테스트 가이드를 확인할 수 있습니다.

- **URL**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **참고**: `SWAGGER_TEST_GUIDE.md`의 최신 가이드 내용이 Swagger UI 상단에 자동으로 주입됩니다.

---

## 프로젝트 구조 (Project Structure)

### Backend Structure

```
backend/
├── src/main/java/com/modu/office/
│   ├── config/              # 전역 설정 (Security, JPA, SSE 등)
│   ├── controller/          # REST API 엔드포인트 도메인별 관리
│   │   └── Auth/            # 고객 및 운영자 전용 인증 컨트롤러
│   ├── service/             # 도메인별 비즈니스 로직 및 트랜잭션 처리
│   ├── repository/          # Spring Data JPA 기반 데이터 접근 계층
│   ├── entity/              # JPA 엔티티 및 스키마 매핑 (enums 포함)
│   ├── dto/                 # Request/Response 데이터 전송 객체
│   ├── security/            # JWT 필터 및 인증/인가 세부 구현체
│   ├── scheduler/           # 스케줄 작업 (결제 자동 취소 등)
│   ├── audit/               # 감사 로그 엔티티 및 저장 로직
│   └── common/              # 전역 예외 처리 및 공통 유틸리티
└── src/main/resources/      # 환경별 프로젝트 설정 파일 (application.yml)
```

### Frontend Structure

```
frontend/
├── src/
│   ├── features/            # 도메인 기반 기능 모듈 (Reservation, Office 등)
│   ├── components/          # 재사용 가능한 UI 컴포넌트
│   ├── layouts/             # 공통 페이지 레이아웃 (Admin, Client)
│   ├── contexts/            # 전역 상태 공유를 위한 Context API
│   ├── routes/              # React Router 기반 페이지 라우팅 설정
│   └── styles/              # CSS 변수 및 전역 스타일 테마
├── package.json             # 프론트엔드 의존성 및 스크립트 설정
└── vite.config.ts           # Vite 빌드 및 개발 환경 설정
```

---

## 주요 기능 (Major Features)

### 자원 관리 (Resource Management)

- **계층 구조 자원 관리**: 지점(Office)과 하위 공간(Room)의 1:N 관계로 기업 내 공간을 체계적으로 구성합니다.
- **복합 조건 필터링**: 수용 인원, 층수, 카테고리, 편의시설 등 다양한 조건으로 공간을 검색할 수 있습니다.
- **위치 기반 탐색**: Google Maps API와 연동된 위경도 기반 반경 검색으로 가까운 지점을 찾을 수 있습니다.

### 예약 엔진 (Reservation Engine)

- **동시성 제어**: JPA `@Version` 기반 낙관적 락으로 동시 예약 요청 중 하나만 수용하고, 나머지는 `OptimisticLockingFailureException`으로 처리합니다.
- **시간대 중복 방지**: DB 제약 조건과 서비스 레이어 이중 검증으로 동일 시간대 중복 예약을 차단합니다.
- **실시간 상태 전파**: SSE로 타 사용자의 예약/취소를 새로고침 없이 즉시 반영합니다.
- **예약 시간 정책**: 30분 단위(00분, 30분)만 허용하며, 자정을 넘기는 예약은 차단됩니다.
- **IDOR 방어**: 예약 조회·수정·취소 시 인증된 사용자와 예약 소유자 일치 여부를 검증합니다.

### 권한 및 보안 체계 (RBAC)

- **계정·프로필 분리**: 인증 계정(Account)과 사용자 프로필(AppUser)을 별도 엔티티로 분리하여 OAuth2 연동과 역할 관리를 유연하게 처리합니다.
- **3단계 역할 제어**:
  - **USER**: 본인 예약 생성·조회·취소
  - **MANAGER**: 담당 지점의 공간 관리, 예약 현황 조회, 점유율 통계
  - **ADMIN**: 전체 사용자 관리, 감사 로그 조회, Manager 승인, 강제 취소

### 감사 로그 및 대시보드 (Audit & Dashboard)

- **감사 로그**: PostgreSQL JSONB로 예약 변경의 Before/After 스냅샷을 기록하여 변경 이력을 추적합니다.
- **운영 대시보드**: 점유율, 취소율, 인기 회의실, 피크타임 등 통계를 관리자 대시보드에서 조회할 수 있습니다.

---

## 데이터베이스 스키마 (Database Schema)

## ERD

<img width="2755" height="1290" alt="Image" src="https://github.com/user-attachments/assets/f5b08439-8d44-4f0b-b07b-cd86975b4ad2" />

### 1. Account (계정)

- **id**: PK, Auto Increment
- **email**: 사용자 이메일 (Unique)
- **password_hash**: 암호화된 비밀번호(LOCAL 로그인 시 사용, OAuth면 NULL 가능)
- **login_type**: 로그인 타입 (LOCAL, NAVER 등)
- **oauth_id**: OAuth 제공자 사용자 식별자 (NAVER user id )
- **status**: 계정 상태 (ACTIVE, SUSPENDED, DELETED)
- **created_at / updated_at**: 생성 / 수정 시각

### 2. AppUser (사용자 프로필)

- **id**: PK, Auto Increment
- **account_id**: Account 테이블 FK (1:1 관계)
- **name**: 사용자 이름
- **role**: 사용자 권한 (USER, MANAGER, ADMIN)
- **approval_status**: 운영자 승인 상태 (PENDING, APPROVED)
- **created_at / updated_at**: 생성 / 수정 시각

### 3. Office (지점)

- **id**: PK, Auto Increment
- **manager_id**: 지점 소유자(운영자) AppUser FK
- **name**: 지점명
- **location**: 지점 위치 (주소)
- **latitude / longitude**: 위도 / 경도
- **open_time / close_time**: 영업 시간
- **open_days**: 영업 요일 배열 (1=월요일, 7=일요일)
- **description**: 지점 상세 설명 (TEXT)
- **created_at / updated_at**: 생성 / 수정 시각

### 4. Room (공간)

- **id**: PK, Auto Increment
- **office_id**: 소속 지점 ID (FK)
- **name**: 공간 이름
- **room_code**: 공간 호수 (ex: 305호)
- **description**: 공간 상세 설명 (TEXT)
- **banner_image_url**: 메인 배너 이미지 URL
- **floor**: 층수
- **capacity**: 수용 인원
- **category**: 공간 카테고리
- **price**: 시간당 가격
- **buffer_time**: 정비 시간 (분 단위)
- **status**: 공간 상태 (AVAILABLE, INACTIVE)
- **version**: 낙관적 락(Optimistic Lock) 버전 관리
- **created_at / updated_at**: 생성 / 수정 시각

### 5. RoomImage (공간 이미지)

- **id**: PK, Auto Increment
- **room_id**: Room FK
- **image_url**: 이미지 경로
- **display_order**: 노출 순서
- **created_at**: 등록 시각

### 6. Facility (설비)

- **id**: PK, Auto Increment
- **facility_code**: 시설 고유 코드 (Unique)
- **facility_name**: 사용자에게 표시될 시설명
- **is_active**: 활성 여부
- **created_at / updated_at**: 생성 / 수정 시각

### 7. Room_Facility (공간-설비 매핑)

- **room_id**: Room FK
- **facility_id**: Facility FK
- **created_at**: 매핑 등록 시각

### 8. Reservation (예약)

- **id**: PK, Auto Increment
- **title**: 예약 제목
- **office_id**: 지점 ID (FK)
- **room_id**: 공간 ID (FK)
- **user_id**: 예약자(사용자) ID (FK)
- **start_at**: 시작 시간
- **end_at**: 종료 시간
- **end_at_include_buffer_time**: 정비 시간을 포함한 종료 시간
- **status**: 예약 상태 (PENDING, CONFIRMED, CANCELED 등)
- **version**: 낙관적 락 버전 관리
- **created_at / updated_at**: 생성 / 수정 시각

### 9. Review (후기)

- **id**: PK, Auto Increment
- **reservation_id**: Reservation FK (Unique → 예약 1건당 후기 1개)
- **author_user_id**: 작성자 AppUser FK
- **rating**: 평점 (1~5)
- **content**: 후기 내용
- **created_at / updated_at**: 생성 / 수정 시각

### 10. FacilityReport (시설 신고)

- **id**: PK, Auto Increment
- **reservation_id**: 예약 ID (FK)
- **room_id**: 공간 ID (FK)
- **facility_id**: 설비 ID (FK)
- **issue_type**: 이슈 유형 Enum — `BROKEN`(고장), `MALFUNCTION`(오작동), `NEEDS_SUPPLIES`(소모품 부족), `DIRTY`(청결불량), `MISSING`(비품 없음), `OTHER`(기타)
- **status**: 처리 상태 — `REPORTED`(접수), `IN_PROGRESS`(처리 중), `RESOLVED`(해결), `CANCELED`(철회)
- **created_at / updated_at**: 생성 / 수정 시각

### 11. UpdateLog (변경 로그)

- **id**: PK, Auto Increment
- **reservation_id**: 관련 예약 ID (FK)
- **actor_user_id**: 변경을 수행한 사용자 ID (FK)
- **action**: 수행된 작업 (CREATE, UPDATE, CANCEL 등)
- **before_data**: 변경 전 데이터 (JSONB)
- **after_data**: 변경 후 데이터 (JSONB)
- **occurred_at**: 발생 시각

### 12. Notification (알림)

- **id**: PK, Auto Increment
- **user_id**: 알림 수신자 ID (FK)
- **content**: 알림 메시지 내용
- **is_read**: 읽음 여부
- **created_at**: 생성 시각

### 13. CancellationPolicy (취소 정책)

- **id**: PK, Auto Increment
- **office_id**: 지점 ID (FK)
- **days_before**: 취소 기준일
- **refund_rate**: 환불 비율 (0~100)
- **created_at / updated_at**: 생성 / 수정 시각

### 14. RefreshToken ( JWT 토큰)

- **id**: PK, Auto Increment
- **token**: RefreshToken (Unique)
- **account_id**: Account FK (Unique → 계정당 Refresh Token 1개 보장)
- **expiry_date**: 만료 시각
- **created_at**: 생성 시각

### 15. RoomFavorite (즐겨찾기)

- **id**: PK, Auto Increment
- **user_id**: 사용자 ID (AppUser FK)
- **room_id**: 공간 ID (Room FK)
- **created_at / updated_at**: 생성 / 수정 시각

### 16. Payment (결제)

- **id**: PK, Auto Increment
- **reservation_id**: Reservation FK (Unique → 결제 1건당 예약 1건)
- **order_id**: 토스페이먼츠 주문 번호 (`rev-{reservation_id}-{random}`)
- **payment_key**: 토스 결제 승인 후 발급되는 고유 키
- **amount**: 실제 결제된 금액
- **status**: 결제 처리 상태 (READY, IN_PROGRESS, DONE, CANCELED, ABORTED 등)
- **method**: 결제 수단 (카드 등)
- **approved_at**: 결제 최종 승인 시각
- **canceled_at**: 결제 취소(환불) 처리 시각
- **fail_reason**: 결제 또는 취소 실패 시 원인 기록
- **created_at / updated_at**: 생성 / 수정 시각
