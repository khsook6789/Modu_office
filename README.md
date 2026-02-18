# 모두의 오피스 (Modu_office)

**Modu_office**는 기업 내 분산된 회의실, 공용 장비 및 각종 시설 자원을 효율적으로 관리하고 예약할 수 있는 **자원 관리 플랫폼**입니다.
단순한 예약 기능을 넘어, 데이터 무결성을 보장하는 고성능 예약 엔진과 실시간 동기화 기술을 통해 기업 운영의 최적화를 지원합니다.

---

## 핵심 목표 (Core Objectives)

- **서비스 안정성 (Stability)**: 낙관적 락(Optimistic Locking) 기술을 도입하여 다수 사용자의 대규모 동시 접근 상황에서도 데이터 충돌 없이 중복 예약을 원천 차단합니다.
- **실시간 응답성 (Responsiveness)**: WebSocket(STOMP) 프로토콜 기반의 실시간 브로드캐스팅을 통해 예약 현황 변화를 즉각적으로 전파하여 매끄러운 사용자 경험을 제공합니다.
- **데이터 신뢰성 (Reliability)**: PostgreSQL의 JSONB 타입을 활용한 정밀 감사 로그 시스템을 구축하여, 모든 자원의 변경 이력을 투명하게 추적하고 데이터 무결성을 보장합니다.
- **아키텍처 확장성 (Scalability)**: 계정(Account)과 프로필(AppUser)을 분리한 유연한 도메인 설계와 세밀한 역할 기반 접근 제어(RBAC)를 통해 가파른 조직 성장에도 문제없이 대응합니다.

---

## 기술 스택 (Tech Stack)

### Backend

| category         | Technology            | Description                                       |
| :--------------- | :-------------------- | :------------------------------------------------ |
| **Framework**    | Spring Boot 3.3       | RESTful API 및 비즈니스 로직 핵심 프레임워크      |
| **Language**     | Java 21               | 현대적 자바 기능 기반의 안정적인 백엔드 코드      |
| **Persistence**  | Spring Data JPA       | 객체 지향적 데이터 접근 및 관계 매핑 관리         |
| **Database**     | PostgreSQL 15         | 고성능 동시성 제어 및 JSONB 타입을 통한 로그 적재 |
| **Security**     | Spring Security + JWT | 무상태(Stateless) 기반의 보안 인증 및 권한 관리   |
| **Real-time**    | WebSocket (STOMP)     | `구현 예정` 예약 현황의 실시간 브로드캐스팅       |
| **Validation**   | Jakarta Validation    | DTO 입력값 검증 및 데이터 무결성 보장             |
| **Testing**      | JUnit 5               | 단위/통합 테스트를 통한 코드 신뢰성 확보          |
| **Logging**      | SLF4J                 | 시스템 동작 추적 및 디버깅을 위한 로깅            |
| **DevOps**       | Spring Boot DevTools  | 핫 리로드 등 개발 생산성 향상 도구                |
| **External API** | Google Maps API       | 지점 위치 정보 및 거리 기반 검색 서비스 제공      |
| **Auxiliary**    | Lombok                | 보일러플레이트 코드 제거를 통한 생산성 향상       |
| **Build Tool**   | Gradle                | 프로젝트 빌드 및 의존성 라이프사이클 관리         |

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

## 프로젝트 구조 (Project Structure)

### Backend Structure

```
backend/
├── src/main/java/com/modu/office/
│   ├── config/              # 전역 설정 (Security, JPA, WebSocket 등)
│   ├── controller/          # REST API 엔드포인트 도메인별 관리
│   │   └── Auth/            # 고객 및 운영자 전용 인증 컨트롤러
│   ├── service/             # 도메인별 비즈니스 로직 및 트랜잭션 처리
│   ├── repository/          # Spring Data JPA 기반 데이터 접근 계층
│   ├── entity/              # JPA 엔티티 및 스키마 매핑 (enums 포함)
│   ├── dto/                 # Request/Response 데이터 전송 객체
│   ├── security/            # JWT 필터 및 인증/인가 세부 구현체
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

- **자산의 체계적 구조화**: 지점(Office)과 하위 공간(Room)의 1:N 관계를 통해 복잡한 기업 내 자산을 직관적이고 계층적으로 관리할 수 있습니다.
- **다차원 공간 필터링**: 수용 인원, 층수, 카테고리 등 상세 속성을 기반으로 사용자가 원하는 최적의 비즈니스 환경을 신속하게 찾아줍니다.
- **위치 지능형 탐색**: Google Maps API와 연동된 위도/경도 기반 검색을 통해, 현 위치에서 가장 가까운 거점 오피스를 추천받아 이동 편의성을 극대화합니다.

### 예약 엔진 (Reservation Engine)

- **동시성 정합성 보장**: JPA `@Version` 기반 낙관적 락을 통해 0.1초 차이로 발생하는 동시 예약 요청 중 단 하나만을 수용하여 데이터 충돌을 원천 해결합니다.
- **지능형 스케줄 충돌 차단**: DB 제약 조건과 서비스 비즈니스 로직의 교차 검증으로 시간대 중복 예약을 완벽히 차단하여 예약 신뢰도를 높입니다.
- **실시간 상태 전파**: WebSocket 프로토콜을 활용하여 타 사용자의 예약 또는 취소 내역을 별도 페이지 새로고침 없이 즉각 화면에 반영해 현황 파악 오류를 방지합니다.

### 권한 및 보안 체계 (RBAC)

- **사용자 중심 보안 설계**: 인증 계정(Account)과 프로필(AppUser)을 분리 설계하여 개인정보 보호를 강화하고 계정 탈취 시에도 핵심 데이터에 대한 피해를 최소화합니다.
- **맥락 인지형 역할 제어**:
  - **CUSTOMER**: 불필요한 기능 노출 없이 본인의 예약 내역과 예약 생성에만 집중할 수 있는 환경을 제공합니다.
  - **OPERATOR**: 담당 지점의 실시간 점유율 조회부터 효율적인 공간 관리까지, 운영의 효율성을 높여주는 관리 도구를 제공합니다.
  - **PLATFORM_ADMIN**: 전사 자원 설정부터 시스템 전반의 로그 모니터링까지, 통합 관제 기능을 통한 아키텍처 거버넌스를 실현합니다.

### 감사 로그 및 대시보드 (Audit & Dashboard)

- **투명한 데이터 거버넌스**: PostgreSQL `JSONB`를 활용하여 예약의 Before/After 스냅샷을 기록함으로써, 모든 변경 이력을 100% 추적 가능한 투명한 운영 환경을 보장합니다.
- **데이터 기반 의사결정 지원**: 축적된 예약률과 점유율 통계를 시각적 대시보드로 제공하여, 공간 운영 전략 수립에 필요한 데이터 인사이트를 즉각 제공합니다.

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

### 2. AppUser (사용자 프로필)

- **id**: PK, Auto Increment
- **account_id**: Account 테이블 FK (1:1 관계)
- **name**: 사용자 이름
- **role**: 사용자 권한 (CUSTOMER, OPERATOR, PLATFORM_ADMIN)

### 3. Office (지점)

- **id**: PK, Auto Increment
- **owner_user_id**: 지점 소유자(운영자) AppUser FK
- **name**: 지점명
- **location**: 지점 위치 (주소)
- **latitude / longitude**: 위도 / 경도
- **open_time / close_time**: 영업 시간
- **open_days**: 영업 요일 배열 (1=월요일, 7=일요일)

### 4. OfficeRoom (회의실/공간)

- **id**: PK, Auto Increment
- **office_id**: 소속 지점 ID (FK)
- **name**: 공간 이름
- **room_code**: 공간 코드 (지점 내 Unique)
- **floor**: 층수
- **capacity**: 수용 인원
- **category**: 공간 카테고리
- **price**: 시간당 가격
- **status**: 공간 상태 (AVAILABLE, INACTIVE)
- **version**: 낙관적 락(Optimistic Lock) 버전 관리

### 5. Facility (설비)

- **id**: PK, Auto Increment
- **name**: 시설 코드/키 (Unique)
- **label**: 표시 이름(설명)
- **is_active**: 활성 여부

### 6. Office_Room_Facility

- **room_id**: OfficeRoom FK
- **facility_id**: Facility FK

### 7. Reservation (예약)

- **id**: PK, Auto Increment
- **title**: 예약 제목
- **office_id**: 지점 ID (FK)
- **room_id**: 공간 ID (FK)
- **customer_id**: 예약자(사용자) ID (FK)
- **start_at**: 시작 시간
- **end_at**: 종료 시간
- **status**: 예약 상태 (PENDING, CONFIRMED, CANCELED 등)
- **version**: 낙관적 락 버전 관리

### 8. Review (후기)

- **id**: PK, Auto Increment
- **reservation_id**: Reservation FK (Unique → 예약 1건당 후기 1개)
- **author_user_id**: 작성자 AppUser FK
- **rating**: 평점 (1~5)
- **content**: 후기 내용

### 9. UpdateLog (변경 로그)

- **id**: PK, Auto Increment
- **reservation_id**: 관련 예약 ID (FK)
- **actor_user_id**: 변경을 수행한 사용자 ID (FK)
- **action**: 수행된 작업 (CREATE, UPDATE, CANCEL 등)
- **before_data**: 변경 전 데이터 (JSONB)
- **after_data**: 변경 후 데이터 (JSONB)
- **occurred_at**: 발생 시각

### 10. RefreshToken (JWT 토큰)

- **id**: PK, Auto Increment
- **token**: RefreshToken (Unique)
- **account_id**: Account FK (Unique → 계정당 Refresh Token 1개 보장)
- **expiry_date**: 만료 시각

### 11. RoomFavorite (즐겨찾기)

- **id**: PK, Auto Increment
- **user_id**: 사용자 ID (AppUser FK)
- **room_id**: 회의실 ID (OfficeRoom FK)
- **created_at**: 등록 시각
- **Unique Constraint**: (user_id, room_id) - 중복 즐겨찾기 방지

---

## API 목록

모든 API는 `/api` 프리픽스를 가집니다.

### 1. 인증 (Auth)

- **Customer**
  - `POST /auth/customer/signup`: 고객 회원가입
  - `POST /auth/customer/login`: 고객 로그인
  - `POST /auth/customer/refresh`: 토큰 갱신
  - `POST /auth/customer/logout`: 고객 로그아웃
- **Operator**
  - `POST /auth/operator/signup`: 운영자 회원가입 (Admin 승인 필요)
  - `POST /auth/operator/login`: 운영자 로그인
  - `POST /auth/operator/refresh`: 토큰 갱신
  - `POST /auth/operator/logout`: 운영자 로그아웃
- **Admin**
  - `POST /auth/admin/login`: 관리자 로그인
  - `POST /auth/admin/refresh`: 토큰 갱신
  - `POST /auth/admin/logout`: 관리자 로그아웃

### 2. 관리자 - Operator 승인 관리 (Admin)

- `GET /admin/operators/pending`: 승인 대기 중인 Operator 목록 조회
- `PATCH /admin/operators/{id}/approve`: Operator 승인 처리

### 3. 지점 관리 (Office)

- `POST /offices`: 지점 생성 (Operator/Admin)
- `GET /offices`: 전체 지점 조회
- `GET /offices/{id}`: 특정 지점 조회
- `PUT /offices/{id}`: 지점 정보 수정 (본인 지점만)
- `DELETE /offices/{id}`: 지점 삭제 (본인 지점만)
- `GET /offices/search`: 지점 검색 (키워드 또는 위치 기반)
- `GET /offices/my-offices`: 내 담당 지점 목록 조회 (Operator/Admin)

### 4. 회의실 관리 (OfficeRoom)

- `POST /offices/{officeId}/rooms`: 공간 생성 (Operator/Admin)
- `GET /offices/{officeId}/rooms`: 지점별 공간 조회 (필터링 가능)
- `GET /rooms/{roomId}`: 특정 공간 조회
- `GET /rooms/search`: 고급 공간 검색 (위치, 예약 가능 여부, 편의시설 등)
- `PUT /rooms/{roomId}`: 공간 정보 수정
- `DELETE /rooms/{roomId}`: 공간 삭제
- `PATCH /offices/{id}/rooms/status`: 회의실 상태 일괄 변경 (Operator/Admin)

### 5. 편의시설 관리 (Facility)

- `POST /admin/facilities`: 편의시설 생성 (Admin)
- `GET /facilities`: 활성 편의시설 목록 조회
- `GET /admin/facilities`: 전체 편의시설 목록 조회 (Admin)
- `GET /admin/facilities/{id}`: 편의시설 상세 조회 (Admin)
- `PUT /admin/facilities/{id}`: 편의시설 수정 (Admin)
- `DELETE /admin/facilities/{id}`: 편의시설 삭제 (Admin)

### 6. 예약 관리 (Reservation)

- `POST /reservations`: 예약 생성
- `GET /reservations`: 예약 목록 조회 (필터링 가능)
- `GET /reservations/{id}`: 예약 상세 조회
- `PUT /reservations/{id}`: 예약 수정
- `PATCH /reservations/{id}/confirm`: 예약 확정
- `POST /reservations/{id}/cancel`: 예약 취소

### 7. 관리자 - 예약 관리 (Admin Reservation)

- `POST /admin/reservations/{id}/force-cancel`: 관리자 권한 예약 강제 취소 (Operator/Admin)

### 8. 감사 로그 (UpdateLog)

- `GET /logs`: 전체 로그 조회
- `GET /logs/reservation/{reservationId}`: 특정 예약 로그 조회

### 9. 즐겨찾기 (RoomFavorite)

- `POST /favorites`: 즐겨찾기 추가
- `DELETE /favorites/{roomId}`: 즐겨찾기 삭제
- `GET /favorites`: 내 즐겨찾기 목록 조회
- `GET /favorites/check/{roomId}`: 즐겨찾기 여부 확인

### 10. 사용자 프로필 (User Profile)

- `GET /users/me`: 내 정보 조회
- `PUT /users/me`: 내 정보 수정 (이름 변경)
- `PUT /users/me/password`: 비밀번호 변경
- `DELETE /users/me`: 회원탈퇴 (소프트 삭제)

---

## 개발 가이드 - 커밋 메시지 (Commit Message Convention)

| 태그       | 설명                                      |
| ---------- | ----------------------------------------- |
| `feat`     | 새로운 기능 추가                          |
| `fix`      | 버그 수정                                 |
| `docs`     | 문서 수정                                 |
| `style`    | 코드 스타일 변경 (포맷, 세미콜론 누락 등) |
| `refactor` | 코드 리팩토링                             |
| `test`     | 테스트 추가/수정                          |
| `chore`    | 빌드 설정, 의존성 업데이트                |
