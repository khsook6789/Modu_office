import { userJourney, managerJourney, browseJourney } from '../helpers/journey.js';

export const options = {
    vus: 100,
    duration: '5m',
    thresholds: {
        http_req_duration: ['p(95)<500'],
        server_errors: ['rate<0.05'], // 정상 부하 시 5xx 에러율 5% 미만
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
