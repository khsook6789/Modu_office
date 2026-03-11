# 🏢 Modu Office API Documentation & Guide

이 문서는 **Modu Office** 시스템의 API를 이해하고 효율적으로 사용하기 위한 종합 가이드입니다.

---

### 🔐 1. 인증 및 보안 가이드 (Authentication)

우리 시스템은 **JWT (JSON Web Token)**를 사용한 Bearer 인증 방식을 채택하고 있습니다. 모든 보호된 API를 호출하기 위해서는 'Authorize' 버튼을 통해 토큰을 주입해야 합니다.

#### 🔌 [Step-by-Step] 토큰 발급 및 적용
1.  **로그인**: `User Auth` 또는 `Admin Auth` 카테고리에서 로그인 API를 호출합니다.
2.  **토큰 복사**: 응답 데이터(`data.accessToken`)의 **문자열값만 복사**합니다 (따옴표 제외).
3.  **Authorize 클릭**: 페이지 상단의 🔓 **Authorize** 버튼을 클릭합니다.
4.  **값 입력**: `Value` 칸에 `Bearer {토큰}` 형식으로 입력합니다.
    *   *예시: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`*
5.  **완료**: 자물쇠가 🔒 **닫힌 모양**으로 바뀌면 모든 호출 시 헤더에 토큰이 자동 포함됩니다.

#### 🎭 권한별 접근 범위 (RBAC)
| 역할 (Role) | 주요 권한 | 비고 |
| :--- | :--- | :--- |
| **USER** | 공간 검색, 내 예약 관리, 리뷰 작성 | 일반 사용자 |
| **MANAGER** | 담당 지점 운영, 예약 승인/거절 | 지점 운영자 |
| **ADMIN** | 시스템 전체 관리, 감사 로그 조회 | 플랫폼 관리자 |

---

### 📡 2. 실시간 통신 가이드 (SSE)

우리 프로젝트는 알림 및 실시간 데이터 동기화를 위해 **SSE (Server-Sent Events)**를 사용합니다.

*   **구독 API**: `/api/notifications/subscribe` (전체 알림)
*   **특이사항**: Swagger UI에서는 스트리밍 연결이 시각적으로 유지되지 않을 수 있습니다. 실시간 연동 테스트는 프론트엔드 환경이나 브라우저 주소창 직접 접속을 권장합니다.

---

### 💬 3. 공통 응답 규격 (Response Format)

모든 API 응답은 아래의 표준 JSON 구조를 100% 준수합니다.

```json
{
  "status": "SUCCESS | ERROR", 
  "code": "200 | 400 | 500",   
  "message": "사용자 메시지 (한글)", 
  "data": { ... }              
}
```

---

### 🚨 4. Error Code Dictionary (에러 사전)

시스템에서 발생하는 주요 비즈니스 에러 코드와 그 의미입니다.

| 코드 | 상태 | 메시지 (예시) | 설명 |
| :--- | :---: | :--- | :--- |
| **401** | Unauthorized | "인증이 필요합니다." | 토큰 누락 또는 만료 |
| **403** | Forbidden | "권한이 없습니다." | 접근 불가능한 리소스 요청 |
| **404** | Not Found | "존재하지 않는 리소스입니다." | ID 오류 또는 삭제된 데이터 |
| **409** | Conflict | "중복된 요청입니다." | 이미 예약된 시간, 중복 찜 등 |
| **500** | Server Error | "서버 내부 오류입니다." | 예상치 못한 시스템 장애 |

---

> [!IMPORTANT]
> **API 최신화 방법**: 테스트 코드 수정 후 `./gradlew openapi3` 명령어를 실행하면 이 문서의 내용과 테스트 결과가 결합되어 최신 Spec으로 갱신됩니다.
