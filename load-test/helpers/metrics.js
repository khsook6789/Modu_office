import { Rate } from 'k6/metrics';

/**
 * 커스텀 메트릭 정의
 *
 * server_errors: 5xx 에러 (실제 서버 장애)
 * business_rejects: 409 Conflict, 400 Bad Request 등 비즈니스 로직에 의한 거절
 * auth_failures: 401 Unauthorized, 403 Forbidden (인증/인가 실패)
 * not_found: 404 Not Found (리소스 미존재)
 * (동시성 제어 결과인 409는 business_rejects로 분류하여 에러율 혼동 방지)
 */
export const serverErrorRate = new Rate('server_errors');
export const businessRejectRate = new Rate('business_rejects');
export const authFailureRate = new Rate('auth_failures');
export const notFoundRate = new Rate('not_found');

/**
 * 응답 상태 코드에 따른 메트릭 기록
 * @param {object} res k6 response 객체
 */
export function recordMetrics(res) {
    // 5xx는 서버 에러
    serverErrorRate.add(res.status >= 500);

    // 401/403은 인증/인가 실패
    authFailureRate.add(res.status === 401 || res.status === 403);

    // 404는 리소스 미존재
    notFoundRate.add(res.status === 404);

    // 409(충돌), 400(잘못된 요청) 등은 비즈니스 거절
    businessRejectRate.add(res.status === 409 || res.status === 400);
}
