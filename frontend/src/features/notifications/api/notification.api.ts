import { BASE_URL } from '../../../api/client';

export interface NotificationResponse {
    id: number;
    type: string;
    message: string;
    isRead: boolean;
    createdAt: string;
    reservationId?: number;
}

const authHeaders = () => {
    const token = localStorage.getItem('token');
    return {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
    };
};

export const notificationApi = {
    getMyNotifications: async (page = 0, size = 20): Promise<{ content: NotificationResponse[]; totalElements: number }> => {
        const res = await fetch(`${BASE_URL}/notifications?page=${page}&size=${size}`, { headers: authHeaders() });
        if (!res.ok) throw new Error('알림 조회 실패');
        const json = await res.json();
        // 응답이 Page 형태: { content, totalElements, ... }
        const data = json.data ?? json;
        return { content: data.content ?? [], totalElements: data.totalElements ?? 0 };
    },

    getUnreadCount: async (): Promise<number> => {
        const res = await fetch(`${BASE_URL}/notifications/unread-count`, { headers: authHeaders() });
        if (!res.ok) return 0;
        const json = await res.json();
        return typeof json === 'number' ? json : (json.data ?? 0);
    },

    markAsRead: async (notificationId: number): Promise<void> => {
        await fetch(`${BASE_URL}/notifications/${notificationId}/read`, {
            method: 'PATCH',
            headers: authHeaders(),
        });
    },

    markAllAsRead: async (): Promise<void> => {
        await fetch(`${BASE_URL}/notifications/read-all`, {
            method: 'PATCH',
            headers: authHeaders(),
        });
    },

    // SSE 구독 — EventSource 반환
    subscribe: (onMessage: (notification: NotificationResponse) => void): EventSource => {
        const token = localStorage.getItem('token');
        const url = `${BASE_URL}/notifications/subscribe${token ? `?token=${encodeURIComponent(token)}` : ''}`;
        const es = new EventSource(url);
        es.onmessage = (e) => {
            try {
                const data = JSON.parse(e.data);
                if (data && data.id) onMessage(data);
            } catch {}
        };
        return es;
    },
};
