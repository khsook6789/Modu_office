import http from 'k6/http';
import { check, sleep } from 'k6';
import { login } from './auth.js';
import { getRandomUserEmail, getRandomManagerEmail, getRandomRoomId, generateReservationSlot, getRandomOfficeId } from './data.js';
import { recordMetrics } from './metrics.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:3000/api';

/**
 * VU별 토큰 캐시 (module-level Map)
 *
 * k6는 각 VU가 독립된 JS 런타임을 사용하므로, module-level 변수는 VU 내에서만 유효함.
 * 즉 이 Map은 사실상 VU당 하나씩 존재하며, iteration 간 토큰을 재사용할 수 있음.
 *
 * 제한 사항: VU 내에서 이메일이 매 iteration마다 랜덤으로 바뀌므로,
 * 동일 VU라도 다른 계정을 사용하면 재로그인이 필요함.
 * 성능 최적화를 위해 VU당 고정 이메일을 사용하려면 __VU 기반 매핑이 필요하나,
 * 현재는 다양한 계정 분포 테스트가 더 중요하므로 iteration별 로그인을 유지함.
 */
const tokenCache = new Map();

/**
 * 캐시된 토큰 조회 또는 로그인 수행
 * 이전 iteration에서 동일 이메일로 로그인한 토큰이 있으면 재사용
 */
function getOrLogin(email, password) {
    const cached = tokenCache.get(email);
    if (cached) {
        // 캐시된 토큰으로 경량 엔드포인트 호출하여 유효성 확인
        const verifyRes = http.get(`${BASE_URL}/offices`, {
            headers: { 'Authorization': `Bearer ${cached}` },
            responseCallback: http.expectedStatuses(200, 401, 403),
        });
        if (verifyRes.status === 200) {
            return cached;
        }
        // 토큰 만료 시 캐시 제거 후 재로그인
        tokenCache.delete(email);
    }

    const token = login(email, password, BASE_URL);
    if (token) {
        tokenCache.set(email, token);
    }
    return token;
}

/**
 * 한국 주요 도시 좌표 (지리적 다양성)
 */
const KOREAN_CITIES = [
    { name: '서울',   lat: 37.5665, lng: 126.978 },
    { name: '부산',   lat: 35.1796, lng: 129.0756 },
    { name: '대전',   lat: 36.3504, lng: 127.3845 },
    { name: '대구',   lat: 35.8714, lng: 128.6014 },
];

function getRandomCity() {
    return KOREAN_CITIES[Math.floor(Math.random() * KOREAN_CITIES.length)];
}

/**
 * 일반 사용자(USER) 여정 - 70% 비중
 */
export function userJourney() {
    const email = getRandomUserEmail();
    const token = getOrLogin(email, 'Test1234!');
    if (!token) return;

    const authHeaders = { headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' } };

    // 1. 지점 검색
    let res = http.get(`${BASE_URL}/offices/search?keyword=Load`, {
        ...authHeaders,
        responseCallback: http.expectedStatuses(200, 400, 404),
    });
    recordMetrics(res);
    check(res, {
        '[지점 검색] status 200': (r) => r.status === 200,
        '[지점 검색] 응답 데이터 존재': (r) => {
            try { 
                const json = r.json();
                return json !== null && (json.data !== undefined || json !== undefined); 
            } catch (e) { 
                return false; 
            }
        },
    });
    sleep(Math.random() * 2 + 1); // 1-3s

    // 2. 주변 회의실 검색 (지리적 다양성 적용)
    const city = getRandomCity();
    res = http.get(`${BASE_URL}/rooms/search?lat=${city.lat}&lng=${city.lng}`, {
        ...authHeaders,
        responseCallback: http.expectedStatuses(200, 400, 404),
    });
    recordMetrics(res);
    check(res, {
        '[회의실 검색] status 200': (r) => r.status === 200,
    });
    sleep(Math.random() * 2 + 1);

    // 3. 회의실 상세 조회
    const roomId = getRandomRoomId();
    res = http.get(`${BASE_URL}/rooms/${roomId}`, {
        ...authHeaders,
        responseCallback: http.expectedStatuses(200, 404),
    });
    recordMetrics(res);
    check(res, {
        '[회의실 상세] status 200': (r) => r.status === 200,
    });
    sleep(Math.random() * 3 + 2); // 2-5s

    // 4. 예약 생성 (미래 시간) - 201 성공 또는 409 시간 충돌 모두 정상 흐름
    const slot = generateReservationSlot();
    const reservationPayload = JSON.stringify({
        roomId: roomId,
        title: 'k6 Load Test Reservation',
        startAt: slot.startAt,
        endAt: slot.endAt
    });
    res = http.post(`${BASE_URL}/reservations`, reservationPayload, {
        ...authHeaders,
        responseCallback: http.expectedStatuses(200, 201, 400, 409),
    });
    recordMetrics(res);
    check(res, {
        '[예약 생성] status 201 또는 409': (r) => r.status === 201 || r.status === 409,
        '[예약 생성] 성공 시 응답 데이터 존재': (r) => {
            if (r.status === 201) {
                try { 
                    const json = r.json();
                    return json !== null && (json.data !== undefined || json !== undefined); 
                } catch (e) { 
                    return false; 
                }
            }
            return true; // 409인 경우 데이터 검증 불필요
        },
    });
    sleep(Math.random() * 1 + 1); // 1-2s

    // 5. 내 예약 목록 조회
    res = http.get(`${BASE_URL}/reservations`, {
        ...authHeaders,
        responseCallback: http.expectedStatuses(200),
    });
    recordMetrics(res);
    check(res, {
        '[내 예약 목록] status 200': (r) => r.status === 200,
    });

    // 6. 예약 취소 시나리오 (~20% 확률)
    if (Math.random() < 0.2) {
        try {
            const body = res.json();
            // 응답 구조에서 예약 목록 추출 (data 배열 또는 최상위 배열)
            const reservations = Array.isArray(body) ? body : (body.data || []);
            if (reservations.length > 0) {
                const target = reservations[Math.floor(Math.random() * reservations.length)];
                const cancelId = target.id || target.reservationId;
                if (cancelId) {
                    sleep(Math.random() * 1 + 0.5); // 0.5-1.5s 사용자 고민 시간
                    const cancelRes = http.post(`${BASE_URL}/reservations/${cancelId}/cancel`, null, {
                        ...authHeaders,
                        responseCallback: http.expectedStatuses(200, 400, 404, 409),
                    });
                    recordMetrics(cancelRes);
                    check(cancelRes, {
                        '[예약 취소] 정상 응답': (r) => r.status === 200 || r.status === 400 || r.status === 409,
                    });
                }
            }
        } catch (e) {
            // Log and skip if response is not JSON or empty
            if (e instanceof Error) {
                console.warn(`Skip cancellation: JSON parsing failed - ${e.message}`);
            }
        }
    }
}

/**
 * 지점 매니저(MANAGER) 여정 - 20% 비중
 */
export function managerJourney() {
    const email = getRandomManagerEmail();
    const token = getOrLogin(email, 'Test1234!');
    if (!token) return;

    const authHeaders = { headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' } };

    // 1. 내 지점 목록 조회
    let res = http.get(`${BASE_URL}/offices/my-offices`, {
        ...authHeaders,
        responseCallback: http.expectedStatuses(200),
    });
    recordMetrics(res);
    sleep(Math.random() * 2 + 1);

    // 2. 예약 현황 검색 (PENDING_APPROVAL 상태)
    res = http.get(`${BASE_URL}/reservations/search?status=PENDING_APPROVAL`, {
        ...authHeaders,
        responseCallback: http.expectedStatuses(200, 400, 404),
    });
    recordMetrics(res);
    sleep(Math.random() * 2 + 2);

    // 3. 예약 확정 - 검색 결과에서 실제 예약 ID를 추출하여 사용
    let confirmed = false;
    try {
        const body = res.json();
        const pendingList = Array.isArray(body) ? body : (body.data || []);
        if (pendingList.length > 0) {
            const target = pendingList[Math.floor(Math.random() * pendingList.length)];
            const resId = target.id || target.reservationId;
            if (resId) {
                const confirmRes = http.patch(`${BASE_URL}/reservations/${resId}/confirm`, null, {
                    ...authHeaders,
                    responseCallback: http.expectedStatuses(200, 400, 404, 409),
                });
                recordMetrics(confirmRes);
                check(confirmRes, {
                    '[예약 확정] 정상 처리': (r) => r.status === 200 || r.status === 409,
                });
                confirmed = true;
            }
        }
    } catch (e) {
        if (e instanceof Error) {
            console.warn(`Skip confirmation: JSON parsing failed - ${e.message}`);
        }
    }

    if (!confirmed) {
        // PENDING 예약이 없는 경우 - 정상적인 상황이므로 로깅만 수행
        sleep(Math.random() * 0.5);
    }

    sleep(Math.random() * 1 + 1);

    // 4. 관리자 통계 조회 (성능 부하 지점)
    res = http.get(`${BASE_URL}/admin/stats/occupancy?officeId=${getRandomOfficeId()}`, {
        ...authHeaders,
        responseCallback: http.expectedStatuses(200, 400, 403, 404),
    });
    recordMetrics(res);
    res = http.get(`${BASE_URL}/admin/stats/peak-times?officeId=${getRandomOfficeId()}`, {
        ...authHeaders,
        responseCallback: http.expectedStatuses(200, 400, 403, 404),
    });
    recordMetrics(res);
    sleep(Math.random() * 2 + 1);
}

/**
 * 비회원 탐색(BROWSE) 여정 - 10% 비중
 */
export function browseJourney() {
    // 1. 전체 지점 목록
    let res = http.get(`${BASE_URL}/offices`, {
        responseCallback: http.expectedStatuses(200),
    });
    recordMetrics(res);
    sleep(Math.random() * 3 + 2);

    // 2. 회의실 검색 (비인증, 지리적 다양성 적용)
    const city = getRandomCity();
    res = http.get(`${BASE_URL}/rooms/search?lat=${city.lat}&lng=${city.lng}`, {
        responseCallback: http.expectedStatuses(200, 400, 404),
    });
    recordMetrics(res);
    sleep(Math.random() * 3 + 2);
}
