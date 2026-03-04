import { client } from '../../../api/client';

interface ApiResponse<T> {
    status: string;
    message: string;
    data: T;
}

export interface AdminUser {
    id: number;
    email: string;
    name: string;
    role: 'USER' | 'MANAGER' | 'ADMIN';
    accountStatus: 'ACTIVE' | 'SUSPENDED' | 'DELETED';
    createdAt: string;
}

export interface PendingManager {
    userId: number;
    name: string;
    email: string;
    approvalStatus: 'PENDING' | 'APPROVED';
    createdAt: string;
}

export interface UpdateLogResponse {
    id: number;
    reservationId: number;
    reservationTitle: string;
    action: string;
    actorId: number;
    actorName: string;
    beforeData: Record<string, any> | null;
    afterData: Record<string, any> | null;
    occurredAt: string;
}

export const adminApi = {
    // 전체 사용자 목록 조회
    getAllUsers: async (): Promise<AdminUser[]> => {
        const response = await client.get<ApiResponse<AdminUser[]>>('/admin/users');
        return response.data;
    },

    // 사용자 계정 정지
    suspendUser: async (id: number): Promise<AdminUser> => {
        const response = await client.patch<ApiResponse<AdminUser>>(`/admin/users/${id}/suspend`);
        return response.data;
    },

    // 사용자 계정 정지 해제
    reactivateUser: async (id: number): Promise<AdminUser> => {
        const response = await client.patch<ApiResponse<AdminUser>>(`/admin/users/${id}/reactivate`);
        return response.data;
    },

    // 승인 대기 중인 MANAGER 목록
    getPendingManagers: async (): Promise<PendingManager[]> => {
        const response = await client.get<ApiResponse<PendingManager[]>>('/admin/managers/pending');
        return response.data;
    },

    // MANAGER 승인
    approveManager: async (id: number): Promise<PendingManager> => {
        const response = await client.patch<ApiResponse<PendingManager>>(`/admin/managers/${id}/approve`);
        return response.data;
    },

    // 감사 로그 조회
    getLogs: async (page = 0, size = 20): Promise<{ content: UpdateLogResponse[], totalElements: number }> => {
        const response = await client.get<ApiResponse<any>>(`/admin/logs?page=${page}&size=${size}`);
        const data = response.data.data ?? response.data; // Page 객체 대응
        return { content: data.content ?? [], totalElements: data.totalElements ?? 0 };
    },
};
