import { type Room } from '../RoomCard';

export interface FacilityResponse {
    id: number;
    facilityCode: string;
    facilityName: string;
    isActive: boolean;
}

// Backend Room Response Structure
export interface RoomResponse {
    id: number;
    officeId: number;
    name: string;
    roomCode: string;
    floor: number;
    status: 'AVAILABLE' | 'RESERVED' | 'MAINTENANCE';
    capacity: number;
    category: 'MEETING_ROOM' | 'CONFERENCE_HALL' | 'FOCUS_ROOM' | 'STUDIO';
    facilities?: FacilityResponse[];
    imageUrl?: string;
    bannerImageUrl?: string;
    images?: { imageUrl: string }[];
    description?: string;
    price: number;
    bufferTime: number;
}

import { client } from '../../../api/client';

// API Response Wrappers
interface ApiResponse<T> {
    status: string;
    message: string;
    data: T;
}

interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

// Helper to map API response to Frontend Room type
export const mapToRoom = (apiRoom: RoomResponse): Room => ({
    id: apiRoom.id.toString(),
    officeId: apiRoom.officeId,
    name: apiRoom.name,
    location: `${apiRoom.floor}F - ${apiRoom.roomCode}`, // Format location
    capacity: apiRoom.capacity,
    equipment: apiRoom.facilities ? apiRoom.facilities.map(f => f.facilityName) : [], // Map facility names
    imageUrl: apiRoom.bannerImageUrl || (apiRoom.images && apiRoom.images.length > 0 ? apiRoom.images[0].imageUrl : undefined) || apiRoom.imageUrl || undefined,
    isAvailable: apiRoom.status === 'AVAILABLE',
    floor: apiRoom.floor,
    roomCode: apiRoom.roomCode,
    description: apiRoom.description,
    price: apiRoom.price,
    bufferTime: apiRoom.bufferTime
});

export interface RoomSearchParams {
    keyword?: string;
    minCapacity?: number;
    facilityNames?: string[];
    minPrice?: number;
    maxPrice?: number;
    size?: number;
}

export const roomApi = {
    getAllRooms: async (): Promise<Room[]> => {
        try {
            const response = await client.get<ApiResponse<PageResponse<RoomResponse>>>('/rooms/search?size=100');
            const pageData = response.data;
            if (pageData && Array.isArray(pageData.content)) {
                return pageData.content.map(mapToRoom);
            }
            return [];
        } catch (error) {
            console.error("Failed to fetch rooms from backend", error);
            return [];
        }
    },
    searchRooms: async (params: RoomSearchParams): Promise<Room[]> => {
        try {
            // facilityNames는 배열이므로 URLSearchParams로 수동 직렬화
            const query = new URLSearchParams();
            if (params.keyword) query.append('keyword', params.keyword);
            if (params.minCapacity) query.append('minCapacity', String(params.minCapacity));
            if (params.minPrice !== undefined) query.append('minPrice', String(params.minPrice));
            if (params.maxPrice !== undefined) query.append('maxPrice', String(params.maxPrice));
            if (params.facilityNames && params.facilityNames.length > 0) {
                params.facilityNames.forEach(name => query.append('facilityNames', name));
            }
            query.append('size', String(params.size ?? 100));

            const response = await client.get<ApiResponse<PageResponse<RoomResponse>>>(`/rooms/search?${query.toString()}`);
            const pageData = response.data;
            if (pageData && Array.isArray(pageData.content)) {
                return pageData.content.map(mapToRoom);
            }
            return [];
        } catch (error) {
            console.error("Failed to search rooms", error);
            return [];
        }
    },
    getSimilarRooms: async (roomId: number): Promise<Room[]> => {
        try {
            const response = await client.get<ApiResponse<RoomResponse[]>>(`/rooms/${roomId}/similar`);
            const data = response.data;
            if (Array.isArray(data)) {
                return data.map(mapToRoom);
            }
            return [];
        } catch (error) {
            console.error("Failed to fetch similar rooms", error);
            return [];
        }
    },
    getActiveFacilities: async (): Promise<FacilityResponse[]> => {
        try {
            const response = await client.get<ApiResponse<FacilityResponse[]>>('/facilities');
            const data = response.data;
            if (Array.isArray(data)) {
                return data;
            }
            return [];
        } catch (error) {
            console.error("Failed to fetch facilities", error);
            return [];
        }
    },
    getRoomsByOffice: async (officeId: number) => {
        const response = await client.get<ApiResponse<RoomResponse[]>>(`/offices/${officeId}/rooms`);
        return response.data;
    },
    getRoomById: async (roomId: number | string) => {
        const response = await client.get<ApiResponse<RoomResponse>>(`/rooms/${roomId}`);
        return response.data;
    },
    createRoom: async (officeId: number, data: any) => {
        const response = await client.post<ApiResponse<RoomResponse>>(`/offices/${officeId}/rooms`, data);
        return response.data;
    },
    updateRoom: async (roomId: number, data: any) => {
        const response = await client.put<ApiResponse<RoomResponse>>(`/rooms/${roomId}`, data);
        return response.data;
    },
    deleteRoom: async (roomId: number) => {
        await client.delete<ApiResponse<void>>(`/rooms/${roomId}`);
    }
};

