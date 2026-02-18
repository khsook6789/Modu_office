import { client } from '../../../api/client';

export interface Office {
    id: number;
    name: string;
    location: string;
    latitude: number;
    longitude: number;
    openTime: string;
    closeTime: string;
}

// API Response Wrappers
interface ApiResponse<T> {
    status: string;
    message: string;
    data: T;
}

export const officeApi = {
    getAllOffices: async () => {
        const response = await client.get<ApiResponse<Office[]>>('/offices');
        return response.data;
    },
    getMyOffices: async () => {
        const response = await client.get<ApiResponse<Office[]>>('/offices/my-offices');
        return response.data;
    },
    getOfficeById: async (id: string | number) => {
        const response = await client.get<ApiResponse<Office>>(`/offices/${id}`);
        return response.data;
    },
    createOffice: async (data: Omit<Office, 'id'>) => {
        const response = await client.post<ApiResponse<Office>>('/offices', data);
        return response.data;
    },
    updateOffice: async (id: string | number, data: Partial<Office>) => {
        const response = await client.put<ApiResponse<Office>>(`/offices/${id}`, data);
        return response.data;
    },
    deleteOffice: async (id: string | number) => {
        await client.delete<ApiResponse<void>>(`/offices/${id}`);
    }
};
