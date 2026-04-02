import { userJourney, managerJourney, browseJourney } from '../helpers/journey.js';

export const options = {
    stages: [
        { duration: '1m', target: 200 },
        { duration: '1m', target: 3000 }, // 과부하 유도
        { duration: '2m', target: 3000 },
        { duration: '2m', target: 200 },  // 부하 해소 후 회복 관찰
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'], // 회복 후 baseline 수준 복귀 검증
        server_errors: ['rate<0.05'],     // 회복 후 서버 에러율 5% 미만
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
