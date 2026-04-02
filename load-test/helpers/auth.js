import http from 'k6/http';
import { check } from 'k6';
import { recordMetrics } from './metrics.js';

/**
 * 사용자/매니저 로그인
 * @param {string} email
 * @param {string} password
 * @param {string} baseUrl
 * @returns {string|null} accessToken
 */
export function login(email, password, baseUrl) {
    const payload = JSON.stringify({
        email: email,
        password: password,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // 이메일에 manager가 포함되어 있으면 매니저 전용 로그인 엔드포인트 호출
    const loginPath = email.includes('manager') ? '/auth/manager/login' : '/auth/user/login';
    const res = http.post(`${baseUrl}${loginPath}`, payload, params);

    // 로그인 실패(5xx 등)도 서버 에러 메트릭에 반영
    recordMetrics(res);

    const success = check(res, {
        'login status is 200': (r) => r.status === 200,
        'has accessToken': (r) => r.json('accessToken') !== undefined,
    });

    if (!success) {
        console.error(`Login failed for ${email}: ${res.status} ${res.body}`);
        return null;
    }

    return res.json('accessToken');
}
