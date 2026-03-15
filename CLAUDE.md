# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Modu Office는 사무 공간, 회의실, 장비 예약을 위한 풀스택 기업 자원 예약 플랫폼입니다. 모노레포 구조로 Spring Boot 백엔드와 React/TypeScript 프론트엔드로 구성됩니다.

## 명령어

### 백엔드 (프로젝트 루트 또는 `backend/` 디렉토리에서)
```bash
./gradlew bootRun          # 백엔드 실행 (application-local.yml 필요)
./gradlew build            # 빌드
./gradlew test             # 테스트 실행
./gradlew openapi3         # OpenAPI 스펙 생성 → src/main/resources/static/openapi3.yaml
```
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### 프론트엔드 (`frontend/` 디렉토리에서)
```bash
npm install
npm run dev      # 개발 서버 실행 (http://localhost:5173)
npm run build    # TypeScript 컴파일 + Vite 번들링
npm run lint     # ESLint 실행
```

## 아키텍처

**백엔드**: Spring Boot 3.5 / Java 21 / PostgreSQL 15
**프론트엔드**: React 19 / TypeScript / Vite / React Router 7

### 백엔드 구조 (`backend/src/main/java/com/modu/office/`)
- `controller/` — 도메인별 REST 엔드포인트 (Auth, Office, Room, Reservation, Payment 등)
- `service/` — 비즈니스 로직 및 트랜잭션 처리
- `repository/` — Spring Data JPA + QueryDSL 레포지토리
- `entity/` — JPA 엔티티 (`Room`, `Reservation`에 `@Version`으로 낙관적 잠금 적용)
- `dto/` — 요청/응답 DTO
- `security/` — JWT 필터 체인, UserDetails 구현체
- `config/` — Security, CORS, JPA, SSE, OAuth2 설정
- `common/` — 전역 예외 핸들러 및 공통 유틸리티
- `scheduler/` — 스케줄 작업 (예: 결제 자동 취소)
- `audit/` — `UpdateLog` 엔티티에 JSONB before/after 스냅샷으로 감사 로그 저장

**DB 마이그레이션**: Flyway (`backend/src/main/resources/db/migration/`)
**실시간 통신**: 알림 및 캘린더 업데이트에 Server-Sent Events (SSE) 사용
**인증**: JWT 기반 무상태 인증, 역할 구분 (USER / MANAGER / ADMIN), 네이버 OAuth2 지원
**결제**: Toss Payments SDK 연동
**지도**: Google Maps API (백엔드 주소 지오코딩 + 프론트엔드 `@react-google-maps/api`)

### 프론트엔드 구조 (`frontend/src/`)
- `features/` — 도메인별 기능 모듈 (Reservation, Office, Room, Admin 등)
- `components/` — 공통 UI 컴포넌트
- `contexts/` — 전역 상태를 위한 React Context (인증 등)
- `layouts/` — Admin / Client 레이아웃 래퍼
- `routes/` — React Router 설정
- `api/` — Axios 기반 API 클라이언트 함수
- `hooks/` — 커스텀 React 훅

## 로컬 개발 환경 설정

1. `backend/src/main/resources/application-local.example.yml` → `application-local.yml`로 복사 후 자격증명 입력
2. `frontend/.env` 파일 생성 후 `VITE_GOOGLE_MAPS_API_KEY=<key>` 입력
3. `localhost:5432`에서 PostgreSQL 실행, `modu_office` 데이터베이스 생성
4. 백엔드 실행: `./gradlew bootRun`
5. 프론트엔드 실행: `cd frontend && npm run dev`

### 백엔드 환경 변수 (`application-local.yml`)
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — PostgreSQL 연결 정보
- `JWT_SECRET` — Base64 인코딩된 32바이트 이상 시크릿
- `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET` — 네이버 OAuth2
- `GOOGLE_MAPS_API_KEY`

### 프론트엔드 환경 변수 (`frontend/.env`)
- `VITE_GOOGLE_MAPS_API_KEY`

## 핵심 도메인 개념

- **Office** — MANAGER 역할 사용자가 관리하는 실제 지점
- **Room** — Office 내 회의실; 카테고리, 수용 인원, 가격, 시설(Facility) 보유
- **Reservation** — Room의 시간대 예약; 이중 예약 방지를 위해 낙관적 잠금 사용
- **Notification** — SSE로 전달되며 DB에 저장
- **CancellationPolicy** — Office별로 정의된 환불 규정
- **Payment** — Reservation에 연결된 Toss Payments 결제 흐름
