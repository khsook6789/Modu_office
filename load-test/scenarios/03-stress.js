import { userJourney, managerJourney, browseJourney } from '../helpers/journey.js';

export const options = {
    stages: [
        { duration: '2m', target: 50 },
        { duration: '3m', target: 200 },
        { duration: '3m', target: 500 },
        { duration: '3m', target: 1000 },
        { duration: '4m', target: 2000 }, // Breaking Point 탐색
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        server_errors: ['rate<0.1'], // 스트레스 상황에서 에러율 10% 미만 유지 시도
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
