import { client } from '../../../api/client';

export interface Facility {
    id: number;
    name: string;
    description: string;
    iconUrl: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface FacilityRequest {
    name: string;
    description?: string;
    iconUrl?: string;
}

export const facilityApi = {
    // 활성 시설 목록 (전체 사용자)
    getActiveFacilities: async (): Promise<Facility[]> => {
        const response = await client.get<any>('/facilities');
        return response.data?.data || response.data || [];
    },

    // 전체 시설 목록 (ADMIN)
    getAllFacilities: async (): Promise<Facility[]> => {
        const response = await client.get<any>('/admin/facilities');
        return response.data?.data || response.data || [];
    },

    // 시설 생성 (ADMIN)
    createFacility: async (data: FacilityRequest): Promise<Facility> => {
        const response = await client.post<any>('/admin/facilities', data);
        return response.data?.data || response.data;
    },

    // 시설 수정 (ADMIN)
    updateFacility: async (id: number, data: FacilityRequest): Promise<Facility> => {
        const response = await client.put<any>(`/admin/facilities/${id}`, data);
        return response.data?.data || response.data;
    },

    // 시설 삭제 (ADMIN)
    deleteFacility: async (id: number): Promise<void> => {
        await client.delete(`/admin/facilities/${id}`);
    }
};
