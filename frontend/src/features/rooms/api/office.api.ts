import { client } from '../../../api/client';

export interface Office {
    id: number;
    name: string;
    description?: string;
    location: string;
    latitude: number;
    longitude: number;
    openTime: string;
    closeTime: string;
    openDays: number[];
}

// API Response Wrappers
interface ApiResponse<T> {
    status: string;
    message: string;
    data: T;
}

export const officeApi = {
    getAllOffices: async (): Promise<Office[]> => {
        const response = await client.get<ApiResponse<Office[]>>('/offices');
        return response.data;
    },
    getMyOffices: async (): Promise<Office[]> => {
        const response = await client.get<ApiResponse<Office[]>>('/offices/my-offices');
        return response.data;
    },
    getOfficeById: async (id: string | number): Promise<Office> => {
        const response = await client.get<ApiResponse<Office>>(`/offices/${id}`);
        return response.data;
    },
    createOffice: async (data: any): Promise<Office> => {
        const response = await client.post<ApiResponse<Office>>('/offices', data);
        return response.data;
    },
    updateOffice: async (id: string | number, data: Partial<Office>): Promise<Office> => {
        const response = await client.put<ApiResponse<Office>>(`/offices/${id}`, data);
        return response.data;
    },
    deleteOffice: async (id: string | number): Promise<void> => {
        await client.delete<ApiResponse<void>>(`/offices/${id}`);
    }
};
