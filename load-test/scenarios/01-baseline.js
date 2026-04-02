import { userJourney, managerJourney, browseJourney } from '../helpers/journey.js';

export const options = {
    vus: 1,
    duration: '60s',
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95% 요청이 500ms 이내 응답
        server_errors: ['rate<0.01'],    // 5xx 에러율 1% 미만
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
