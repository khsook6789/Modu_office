import { userJourney, managerJourney, browseJourney } from '../helpers/journey.js';

export const options = {
    vus: 200,
    duration: '30m', // 장시간 내구 테스트 (Soak)
    thresholds: {
        http_req_duration: ['p(95)<1000'], // 데이터 누적에도 불구하고 1초 미만 응답 유지 목표
        server_errors: ['rate<0.05'],
    },
};

export default function () {
    const rand = Math.random();
    if (rand < 0.7) {
        userJourney();
    } else if (rand < 0.9) {
        managerJourney();
    } else {
        browseJourney();
    }
}
