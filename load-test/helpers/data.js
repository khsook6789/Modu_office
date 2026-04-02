/**
 * 부하 테스트용 동적 데이터 유틸리티
 */

// 시딩된 계정 범위 (loadtest-user-0001 ~ 5000, loadtest-manager-001 ~ 500)
export const USER_COUNT = 5000;
export const MANAGER_COUNT = 500;
export const OFFICE_COUNT = 100;
export const ROOM_COUNT = 500;

export function getRandomUserEmail() {
    const id = Math.floor(Math.random() * USER_COUNT) + 1;
    return `loadtest-user-${String(id).padStart(4, '0')}@test.com`;
}

export function getRandomManagerEmail() {
    const id = Math.floor(Math.random() * MANAGER_COUNT) + 1;
    return `loadtest-manager-${String(id).padStart(3, '0')}@test.com`;
}

/**
 * 30분 단위 스냅된 미래 예약 슬롯 생성
 * @returns {object} { startAt, endAt } (ISO String)
 */
export function generateReservationSlot() {
    const daysAhead = Math.floor(Math.random() * 30) + 1; // 1~30일 후
    const date = new Date();
    date.setDate(date.getDate() + daysAhead);
    
    const hour = Math.floor(Math.random() * 8) + 9; // 09:00 ~ 16:00 시작
    const minute = Math.random() < 0.5 ? 0 : 30;    // 00분 또는 30분 스냅
    
    const startAt = new Date(date);
    startAt.setHours(hour, minute, 0, 0);
    
    const endAt = new Date(startAt);
    endAt.setHours(startAt.getHours() + 1); // 1시간 대여
    
    return {
        startAt: startAt.toISOString(),
        endAt: endAt.toISOString()
    };
}

export function getRandomRoomId() {
    return Math.floor(Math.random() * ROOM_COUNT) + 1;
}

export function getRandomOfficeId() {
    return Math.floor(Math.random() * OFFICE_COUNT) + 1;
}
