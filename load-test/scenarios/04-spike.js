import { userJourney, managerJourney, browseJourney } from '../helpers/journey.js';

export const options = {
    stages: [
        { duration: '30s', target: 100 },
        { duration: '10s', target: 5000 }, // 폭발적인 유입 (Spike)
        { duration: '1m', target: 5000 },  // 유지
        { duration: '2m', target: 100 },   // 급격한 감소
    ],
    thresholds: {
        http_req_duration: ['p(95)<10000'], // 극한 스파이크에서도 p95 10초 이내
        server_errors: ['rate<0.2'],        // 5xx 에러율 20% 미만 (graceful degradation)
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
