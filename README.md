# 모두의 오피스 (Modu_office)

**Modu_office**는 기업 내 분산된 회의실, 공용 장비 및 각종 시설 자원을 효율적으로 관리하고 예약할 수 있는 **자원 관리 플랫폼**입니다.
단순한 예약 기능을 넘어, 데이터 무결성을 보장하는 고성능 예약 엔진과 실시간 동기화 기술을 통해 기업 운영의 최적화를 지원합니다.

---

## 핵심 목표 (Core Objectives)

- **서비스 안정성 (Stability)**: 낙관적 락(Optimistic Locking) 기술을 도입하여 다수 사용자의 대규모 동시 접근 상황에서도 데이터 충돌 없이 중복 예약을 원천 차단합니다.
- **성능 최적화 (Performance)**: JPA `@EntityGraph`와 `Fetch Join`을 전략적으로 활용하여 복잡한 연관 관계 조회 시 발생하는 N+1 문제를 원천 해결하고 쿼리 응답 속도를 극대화했습니다.
- **실시간 응답성 (Responsiveness)**: SSE(Server-Sent Events) 프로토콜을 도입하여 예약 현황 변화 및 실시간 알림을 새로고침 없이 즉각적으로 전파하여 매끄러운 사용자 경험을 제공합니다.
- **데이터 신뢰성 (Reliability)**: PostgreSQL의 JSONB 타입을 활용한 정밀 감사 로그 시스템을 구축하여, 모든 자원의 변경 이력을 투명하게 추적하고 데이터 무결성을 보장합니다.
- **아키텍처 확장성 (Scalability)**: 계정(Account)과 프로필(AppUser)을 분리한 유연한 도메인 설계와 세밀한 역할 기반 접근 제어(RBAC)를 통해 가파른 조직 성장에도 문제없이 대응합니다.

---

## 기술 스택 (Tech Stack)

### Backend

| category          | Technology                 | Description                                        |
| :---------------- | :------------------------- | :------------------------------------------------- |
| **Framework**     | Spring Boot 3.5.10         | RESTful API 및 비즈니스 로직 핵심 프레임워크       |
| **Language**      | Java 21                    | 현대적 자바 기능 기반의 안정적인 백엔드 코드       |
| **Persistence**   | Spring Data JPA            | 객체 지향적 데이터 접근 및 관계 매핑 관리          |
| **Database**      | PostgreSQL 15              | 고성능 동시성 제어 및 JSONB 타입을 통한 로그 적재  |
| **Security**      | Spring Security + JWT      | 무상태(Stateless) 기반의 보안 인증 및 권한 관리    |
| **Real-time**     | Server-Sent Events (SSE)   | 예약 현황 및 실시간 알림의 단방향 스트리밍 구현    |
| **Validation**    | Jakarta Validation         | DTO 입력값 검증 및 데이터 무결성 보장              |
| **Testing**       | JUnit 5                    | 단위/통합 테스트를 통한 코드 신뢰성 확보           |
| **Logging**       | SLF4J                      | 시스템 동작 추적 및 디버깅을 위한 로깅             |
| **DevOps**        | Spring Boot DevTools       | 핫 리로드 등 개발 생산성 향상 도구                 |
| **External API**  | Google Maps API            | 지점 위치 정보 및 거리 기반 검색 서비스 제공       |
|                   | Toss Payments              | 결제 위젯 및 API 연동을 통한 온라인 안전 결제 처리 |
| **Auxiliary**     | Lombok                     | 보일러플레이트 코드 제거를 통한 생산성 향상        |
| **Build Tool**    | Gradle                     | 프로젝트 빌드 및 의존성 라이프사이클 관리          |
| **Documentation** | Spring REST Docs + Swagger | TDD 기반의 신뢰성 높은 자동화 API 명세서           |

---

## 🚀 시작하기 (Getting Started)

### API 문서 (Swagger UI)

서버 실행 후 아래 주소에서 전체 API 명세 및 테스트 가이드를 확인할 수 있습니다.

- **URL**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **참고**: `SWAGGER_TEST_GUIDE.md`의 최신 가이드 내용이 Swagger UI 상단에 자동으로 주입됩니다.

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
│   ├── config/              # 전역 설정 (Security, JPA, SSE 등)
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
- **실시간 상태 전파**: SSE(Server-Sent Events) 프로토콜을 활용하여 타 사용자의 예약 또는 취소 내역 및 실시간 알림을 별도 페이지 새로고침 없이 즉각 화면에 반영해 현황 파악 오류를 방지합니다.
- **엄격한 시간 정책**: 모든 예약은 30분 단위(00분, 30분)로만 가능하며, 운영 안정성을 위해 자정(Overnight)을 넘기는 예약은 시스템적으로 차단됩니다.
- **소유권 기반 접근 제어 (IDOR 방어)**: 예약 상세 정보 조회, 수정 및 취소 시 현재 인증된 사용자와 예약 소유자의 일치 여부를 검증하여 비정상적인 접근을 원천 차단합니다.

### 권한 및 보안 체계 (RBAC)

- **사용자 중심 보안 설계**: 인증 계정(Account)과 프로필(AppUser)을 분리 설계하여 개인정보 보호를 강화하고 계정 탈취 시에도 핵심 데이터에 대한 피해를 최소화합니다.
- **맥락 인지형 역할 제어**:
  - **USER**: 불필요한 기능 노출 없이 본인의 예약 내역과 예약 생성에만 집중할 수 있는 환경을 제공합니다.
  - **MANAGER**(오피스 운영자): 담당 지점의 실시간 점유율 조회부터 효율적인 공간 관리까지, 운영의 효율성을 높여주는 관리 도구를 제공합니다.
  - **ADMIN**(플랫폼 관리자): 전사 자원 설정부터 시스템 전반의 로그 모니터링까지, 통합 관제 기능을 통한 아키텍처 거버넌스를 실현합니다.

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

---

## API 목록

### 1. 인증 (Auth)

- **User**
  - `POST /api/auth/user/signup`: 고객 회원가입
  - `POST /api/auth/user/login`: 고객 로그인
  - `POST /api/auth/user/refresh`: 토큰 갱신
  - `POST /api/auth/user/logout`: 고객 로그아웃
- **Manager**
  - `POST /api/auth/manager/signup`: 운영자 회원가입 (Admin 승인 필요)
  - `POST /api/auth/manager/login`: 운영자 로그인
  - `POST /api/auth/manager/refresh`: 토큰 갱신
  - `POST /api/auth/manager/logout`: 운영자 로그아웃
- **Admin**
  - `POST /api/auth/admin/login`: 관리자 로그인
  - `POST /api/auth/admin/refresh`: 토큰 갱신
  - `POST /api/auth/admin/logout`: 관리자 로그아웃

### 2. 관리자 - Manager 승인 관리 (Admin)

- `GET /api/admin/managers/pending`: 승인 대기 중인 Manager 목록 조회
- `PATCH /api/admin/managers/{id}/approve`: Manager 승인 처리 (승인 시 권한 부여)

### 3. 지점 관리 (Office)

- `POST /api/offices`: 지점 생성 (Manager/Admin)
- `GET /api/offices`: 전체 지점 조회
- `GET /api/offices/{id}`: 특정 지점 조회
- `PUT /api/offices/{id}`: 지점 정보 수정 (Manager/Admin, 소유권 확인)
- `DELETE /api/offices/{id}`: 지점 삭제 (Manager/Admin, 소유권 및 예약 가능 여부 확인)
- `GET /api/offices/search`: 지점 복합 검색 (키워드, 위치, 위경도 반경 거리 기반 통합)
- `GET /api/offices/my-offices`: 내 담당 지점 목록 조회 (Manager/Admin)

### 4. 회의실 관리 (Room)

- `POST /api/offices/{officeId}/rooms`: 특정 지점에 새 공간 등록 (Manager/Admin)
- `GET /api/offices/{officeId}/rooms`: 특정 지점의 모든 회의실 조회 (상태/인원 필터링 지원)
- `GET /api/rooms/{roomId}`: 특정 공간 상세 조회
- `GET /api/rooms/search`: 고급 공간 검색 (예약 가능 여부, 날짜, 시간, 위치, 시설 등 복합 필터)
- GET /api/rooms/{roomId}/similar: 유사 회의실 목록 추천 (Collaborative Filtering + 거리/시설 가중치 알고리즘 기반)
- `PUT /api/rooms/{roomId}`: 공간 정보 수정 (Manager/Admin)
- `DELETE /api/rooms/{roomId}`: 공간 삭제 (Manager/Admin)
- `PATCH /api/offices/{id}/rooms/status`: 지점 내 회의실 상태 일괄 변경 (Manager/Admin)
- `PUT /api/rooms/{roomId}/images`: 회의실 이미지 일괄 등록/수정 (URL 기반, displayOrder 지원)
- `DELETE /api/rooms/{roomId}/images/{imageId}`: 특정 회의실 이미지 삭제

### 5. 편의시설 관리 (Facility)

- `POST /api/admin/facilities`: 편의시설 생성 (Admin)
- `GET /api/facilities`: 활성 편의시설 목록 조회
- `GET /api/admin/facilities`: 전체 편의시설 목록 조회 (Admin)
- `GET /api/admin/facilities/{id}`: 편의시설 상세 조회 (Admin)
- `PUT /api/admin/facilities/{id}`: 편의시설 수정 (Admin)
- `DELETE /api/admin/facilities/{id}`: 편의시설 삭제 (Admin)

### 6. 예약 관리 (Reservation)

- `POST /api/reservations`: 새 예약 신청
- `GET /api/reservations`: 예약 목록 조회 (일반 사용자는 본인 것만 강제 필터링, customerId/roomId 필터 지원)
- `GET /api/reservations/{id}`: 특정 예약 상세 조회 (IDOR 방어 포함)
- `PUT /api/reservations/{id}`: 예약 정보(시간/상태) 수정
- `PATCH /api/reservations/{id}/confirm`: 예약 확정 알림 (Manager/Admin 전용)
- `POST /api/reservations/{id}/cancel`: 예약 취소 (환불 규정 자동 적용, 중복 취소 방지)
- `GET /api/reservations/search`: 오퍼레이터용 예약 목록 검색 (Manager/Admin 전용)
- `GET /api/reservations/{id}/refund-preview`: 취소 시 환불 예상액 미리보기

### 7. 관리자 - 예약 관리 (Admin Reservation)

- `POST /api/admin/reservations/{id}/force-cancel`: 관리자 권한 예약 강제 취소 (커스텀 환불 비율 지정 가능)

### 8. 감사 로그 (Audit Log - Admin 전용)

- `GET /api/admin/logs`: 전체 감사 로그 조회 (페이징 지원)
- `GET /api/admin/logs/search`: 감사 로그 정밀 검색 (예약 ID, 변경 필드값, PostgreSQL JSONB 연산 검색 지원)

### 9. 즐겨찾기 (RoomFavorite)

- `POST /api/favorites`: 즐겨찾기 추가
- `DELETE /api/favorites/{roomId}`: 즐겨찾기 삭제
- `GET /api/favorites`: 내 즐겨찾기 목록 조회
- `GET /api/favorites/check/{roomId}`: 특정 공간 즐겨찾기 여부 확인

### 10. 결제 관리 (Payment)

- `POST /api/payments/confirm`: 토스페이먼츠 결제 승인 요청 (결제 위젯 인증 완료 후 호출)
- `GET /api/payments/{reservationId}`: 특정 예약건의 결제 내역 조회 (접근 권한 확인 필수)
  _(참고: 관리자의 예약 강제 취소 API 호출 시 내부적으로 토스 결제 환불 로직이 자동 실행됩니다.)_

### 11. 사용자 프로필 (User Profile)

- `GET /api/users/me`: 내 정보 조회
- `PUT /api/users/me`: 내 정보 수정 (이름 변경)
- `PUT /api/users/me/password`: 비밀번호 변경
- `DELETE /api/users/me`: 회원탈퇴 (소프트 삭제)

### 11. 관리자 - 사용자 관리 (Admin User Management)

- `GET /api/admin/users`: 전체 사용자 목록 조회 (ADMIN 본인 제외, 페이징 지원)
- `PATCH /api/admin/users/{id}/suspend`: 사용자 계정 일시 정지 (로그인 차단)
- `PATCH /api/admin/users/{id}/reactivate`: 사용자 계정 정지 해제 및 활성화

### 12. 후기 관리 (Review)

- `POST /api/reviews`: 공간 예약 후기 작성 (사용 이력 확인 필수)
- `GET /api/reviews/room/{roomId}`: 특정 공간의 후기 목록 조회 (페이징, 최신순)
- `GET /api/reviews/room/{roomId}/summary`: 특정 공간의 후기 요약(평균 별점, 전체 리뷰 수) 조회
- `GET /api/reviews/me`: 내 후기 목록 조회
- `PATCH /api/reviews/{reviewId}`: 후기 내용 및 평점 수정
- `DELETE /api/reviews/{reviewId}`: 후기 삭제

### 13. 알림 관리 (Notification)

- `GET /api/notifications`: 내 알림 목록 조회 (페이징 지원)
- `GET /api/notifications/unread-count`: 읽지 않은 알림 개수 실시간 조회
- `PATCH /api/notifications/{notificationId}/read`: 단일 알림 읽음 처리
- `PATCH /api/notifications/read-all`: 모든 알림 일괄 읽음 처리
- `GET /api/notifications/subscribe`: 실시간 개인 알림 SSE 스트림 구독
- `GET /api/notifications/subscribe/rooms/{roomId}`: 특정 회의실 캘린더 업데이트 SSE 스트림 구독

### 14. 관리자 대시보드 통계 (Admin Dashboard Stats)

- `GET /api/admin/stats/occupancy`: 실시간 점유율 조회 (MANAGER는 officeId 필수, ADMIN은 선택)
- `GET /api/admin/stats/cancellations`: 취소율 통계 조회
- `GET /api/admin/stats/rooms/popular`: 인기 회의실 Top 5 조회
- `GET /api/admin/stats/rooms/unpopular`: 비인기 회의실 Top 5 조회
- `GET /api/admin/stats/peak-times`: 시간대별 예약 분포(피크타임) 조회
- `GET /api/admin/stats/daily-usage`: 일일 총 사용 시간 추이 조회

### 15. 시설 신고 (Facility Report)

- `POST /api/rooms/{roomId}/reports`: 시설 고장 신고 접수 (USER, 예약 시작 이후만 가능, 중복 차단)
- `GET /api/my-reports?reservationId={id}`: 내 예약 신고 내역 조회 (USER)
- `GET /api/offices/{officeId}/reports`: 오피스 신고 내역 전체 조회 (MANAGER/ADMIN)
- `PATCH /api/reports/{reportId}/status`: 신고 처리 상태 변경 (MANAGER/ADMIN, IN_PROGRESS 전환 시 시설 자동 비활성화)
- `PATCH /api/reports/{reportId}/cancel`: 신고 철회 (USER, REPORTED 상태에서만 가능)

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
