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

// Define RoomFilter interface if it's not already defined elsewhere
// Assuming RoomFilter might look something like this based on the commented code
interface RoomFilter {
    capacity?: number;
    // Add other filter properties as needed
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

export const roomApi = {
    getAllRooms: async (filter?: RoomFilter): Promise<Room[]> => { 
        const params = new URLSearchParams();
        
        if (filter?.capacity) params.append('capacity', filter.capacity.toString());

        // Use real backend search endpoint
        // Backend default page size might be 20. We use 100 to get "all".
        // 응답 구조: axios response.data = { status, message, data: { content: [...] } }
        try {
            const response = await client.get<ApiResponse<PageResponse<RoomResponse>>>('/rooms/search?size=100');
            
            // ApiResponse wrapper: response.data.data.content
            const pageData = (response.data as any)?.data;
            if (pageData && pageData.content) {
                return pageData.content.map(mapToRoom);
            }
            // Fallback: 혹시 data가 바로 PageResponse인 경우
            if (response.data && (response.data as any).content) {
                return (response.data as any).content.map(mapToRoom);
            }
            return [];
        } catch (error) {
            console.error("Failed to fetch rooms from backend", error);
            return [];
        }
    },
    getActiveFacilities: async (): Promise<FacilityResponse[]> => {
        try {
            const response = await client.get<ApiResponse<FacilityResponse[]>>('/facilities');
            // Depending on interceptor, response might be the data itself or AxiosResponse
            const resData = response.data as any;
            if (resData && resData.data) {
                return resData.data;
            }
            if (Array.isArray(resData)) {
                return resData;
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
        // Return inner data if wrapped in ApiResponse
        const resData = response.data as any;
        return resData?.data || resData;
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
