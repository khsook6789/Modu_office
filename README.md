# 모두의 오피스 (Modu_office)

**Modu_office**는 회의실과 공용 공간을 예약하고 관리하는 **웹 기반 예약 및 운영 관리 시스템**입니다.
동시성 제어, 실시간 알림, 결제 연동, 운영 대시보드를 갖춘 완전한 예약 서비스입니다.

---

<!-- 📸 스크린샷/데모 자리 — 추후 추가 예정 -->

---

## 주요 기능 (Features)

### 고객 사용자

- **회의실 검색 및 예약**: QueryDSL 기반 다중 조건 필터링(위치, 인원, 제공 항목, 가격)과 Google Maps API 연동 반경 검색으로 회의실을 탐색하고 예약합니다.
- **유사 회의실 추천**: 거리(Haversine), 시설 유사도, 과거 예약 통계를 결합한 가중치 기반 대안 회의실을 추천합니다.
- **실시간 예약 현황**: SSE(Server-Sent Events)로 다른 사용자의 예약/취소를 새로고침 없이 즉시 반영합니다.
- **결제 및 환불**: 토스페이먼츠 결제 위젯 연동과 지점별 취소 정책에 따른 차등 환불을 제공합니다.
- **Naver OAuth 로그인**: 네이버 소셜 로그인을 통한 간편 회원가입 및 인증을 지원합니다.
- **후기 및 즐겨찾기**: 이용 완료된 예약에 대한 후기 작성과 회의실 즐겨찾기 기능을 제공합니다.

### 운영자 (Manager)

- **공간 관리**: 담당 지점의 회의실 정보, 가격, 장비, 이미지를 관리합니다.
- **예약 Buffer Time 설정**: 회의실별 정비 시간을 설정하여 예약 간 자동으로 여유 시간을 확보합니다.
- **운영 대시보드**: 점유율, 취소율, 인기 회의실, 피크타임 등 통계를 조회합니다.
- **시설 신고 관리**: 고객의 시설 고장 신고를 접수하고 처리 상태를 추적합니다.

### 관리자 (Admin)

- **사용자 관리**: 회원 조회, 운영자 승인, 계정 정지/복구를 처리합니다.
- **감사 로그**: PostgreSQL JSONB 기반 Before/After 스냅샷으로 예약 변경 이력을 추적합니다.
- **예약 강제 취소**: 사유 기록과 함께 관리자 권한으로 예약을 강제 취소합니다.

---

## 기술 하이라이트

| 해결한 문제                  | 적용 기술                                                  |
| ---------------------------- | ---------------------------------------------------------- |
| 동시 예약 요청 시 중복 발생  | JPA `@Version` 낙관적 락 + DB 레벨 이중 검증               |
| 예약 현황의 실시간 동기화    | SSE 기반 이벤트 스트리밍 (WebSocket에서 전환)              |
| 예약 검증 규칙의 산발적 분산 | 전략 패턴(Strategy Pattern) 기반 규칙 엔진                 |
| API 문서의 신뢰성과 편의성   | Spring REST Docs(테스트 검증) + Swagger UI(탐색) 이중 구조 |
| 예약 변경 이력 추적          | PostgreSQL JSONB Before/After 스냅샷 감사 로그             |
| 역할별 기능 접근 제어        | Spring Security RBAC (USER / MANAGER / ADMIN)              |
| 연관 엔티티 조회 시 N+1 쿼리 | EntityGraph + Fetch Join + @BatchSize 선택적 적용          |

---

## 기술 스택 (Tech Stack)

### Backend

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Spring REST Docs](https://img.shields.io/badge/REST_Docs-6DB33F?style=flat-square&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_15-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-4285F4?style=flat-square&logoColor=white)
![Caffeine](https://img.shields.io/badge/Caffeine_Cache-FF6B6B?style=flat-square&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0000?style=flat-square&logo=flyway&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)
![SSE](https://img.shields.io/badge/SSE-FF6600?style=flat-square&logoColor=white)
![JUnit 5](https://img.shields.io/badge/JUnit_5-25A162?style=flat-square&logo=junit5&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger_UI-85EA2D?style=flat-square&logo=swagger&logoColor=black)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white)

### Frontend

![React](https://img.shields.io/badge/React_19-61DAFB?style=flat-square&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite_7-646CFF?style=flat-square&logo=vite&logoColor=white)
![React Router](https://img.shields.io/badge/React_Router_7-CA4245?style=flat-square&logo=reactrouter&logoColor=white)
![Axios](https://img.shields.io/badge/Axios-5A29E4?style=flat-square&logo=axios&logoColor=white)
![Vanilla CSS](https://img.shields.io/badge/Vanilla_CSS-1572B6?style=flat-square&logo=css3&logoColor=white)

### External API

![Google Maps API](https://img.shields.io/badge/Google_Maps-4285F4?style=flat-square&logo=googlemaps&logoColor=white)
![Toss Payments](https://img.shields.io/badge/Toss_Payments-0066FF?style=flat-square&logoColor=white)
![Naver OAuth](https://img.shields.io/badge/Naver_OAuth-03C75A?style=flat-square&logo=naver&logoColor=white)

---

## 🚀 시작하기 (Getting Started)

### 1. 환경변수 준비

| 파일                                               | 용도                | 참고                                 |
| :------------------------------------------------- | :------------------ | :----------------------------------- |
| `.env`                                             | Docker DB 컨테이너  | `.env.example` 참고                  |
| `backend/src/main/resources/application-local.yml` | 백엔드 실행         | `application-local.example.yml` 참고 |
| `frontend/.env`                                    | 프론트엔드 지도 API | `VITE_GOOGLE_MAPS_API_KEY` 입력      |

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

## 데이터베이스 스키마 (Database Schema)

### ERD

```mermaid
erDiagram
    ACCOUNT ||--|| APPUSER : has
    ACCOUNT ||--o{ REFRESHTOKEN : generates
    APPUSER ||--o{ OFFICE : manages
    APPUSER ||--o{ RESERVATION : creates
    APPUSER ||--o{ REVIEW : writes
    APPUSER ||--o{ ROOMFAVORITE : saves
    APPUSER ||--o{ UPDATELOG : records

    OFFICE ||--o{ ROOM : contains
    OFFICE ||--o{ CANCELLATIONPOLICY : defines

    ROOM ||--o{ ROOMIMAGE : has
    ROOM ||--o{ ROOMFACILITY : includes
    ROOM ||--o{ RESERVATION : available_for

    FACILITY ||--o{ ROOMFACILITY : belongs_to
    FACILITY ||--o{ FACILITYREPORT : reported_for

    RESERVATION ||--o{ PAYMENT : related_to
    RESERVATION ||--o{ REVIEW : has
    RESERVATION ||--o{ FACILITYREPORT : triggers
    RESERVATION ||--o{ UPDATELOG : changes_tracked

    APPUSER ||--o{ NOTIFICATION : receives

    ACCOUNT {
        bigint id PK
        string email UK
        string password_hash
        string login_type "LOCAL, NAVER"
        string oauth_id
        string status "ACTIVE, SUSPENDED, DELETED"
    }

    APPUSER {
        bigint id PK
        bigint account_id FK
        string name
        string role "USER, MANAGER, ADMIN"
        string approval_status "PENDING, APPROVED"
    }

    OFFICE {
        bigint id PK
        bigint manager_id FK
        string name
        string location
        double latitude
        double longitude
        time open_time
        time close_time
    }

    ROOM {
        bigint id PK
        bigint office_id FK
        string name
        string room_code
        int floor
        int capacity
        string category
        decimal price
        int buffer_time
        string status "AVAILABLE, INACTIVE"
        long version
    }

    ROOMIMAGE {
        bigint id PK
        bigint room_id FK
        string image_url
        int display_order
    }

    FACILITY {
        bigint id PK
        string facility_code UK
        string facility_name
        boolean is_active
    }

    ROOMFACILITY {
        bigint room_id FK
        bigint facility_id FK
    }

    RESERVATION {
        bigint id PK
        string title
        bigint office_id FK
        bigint room_id FK
        bigint user_id FK
        timestamp start_at
        timestamp end_at
        timestamp end_at_include_buffer_time
        string status "PENDING_PAYMENT, PENDING_APPROVAL, CONFIRMED, CANCELED"
        long version
    }

    REVIEW {
        bigint id PK
        bigint reservation_id FK UK
        bigint author_user_id FK
        int rating
        text content
    }

    FACILITYREPORT {
        bigint id PK
        bigint reservation_id FK
        bigint room_id FK
        bigint facility_id FK
        string issue_type "BROKEN, MALFUNCTION, NEEDS_SUPPLIES, DIRTY, MISSING, OTHER"
        string status "REPORTED, IN_PROGRESS, RESOLVED, CANCELED"
    }

    UPDATELOG {
        bigint id PK
        bigint reservation_id FK
        bigint actor_user_id FK
        string action "CREATE, UPDATE, CANCEL"
        jsonb before_data
        jsonb after_data
    }

    NOTIFICATION {
        bigint id PK
        bigint user_id FK
        text content
        boolean is_read
    }

    CANCELLATIONPOLICY {
        bigint id PK
        bigint office_id FK
        int days_before
        int refund_rate
    }

    PAYMENT {
        bigint id PK
        bigint reservation_id FK UK
        string order_id
        string payment_key
        decimal amount
        string status "READY, APPROVED, CANCELED, FAILED"
    }

    ROOMFAVORITE {
        bigint id PK
        bigint user_id FK
        bigint room_id FK
    }

    REFRESHTOKEN {
        bigint id PK
        string token UK
        bigint account_id FK UK
        timestamp expiry_date
    }
```

---

## 팀원 (Team)

| 역할         | 이름               | 담당 영역       | GitHub                                  |
| ------------ | ------------------ | --------------- | --------------------------------------- |
| **Backend**  | 오준서(piker0925)  | 백엔드 개발     | [GitHub](https://github.com/piker0925)  |
| **Backend**  | 이진환(khsook6789) | 백엔드 개발     | [GitHub](https://github.com/khsook6789) |
| **Frontend** | 문윤성(mys0423)    | 프론트엔드 개발 | [GitHub](https://github.com/mys0423)    |
