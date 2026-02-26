import { type Room } from '../RoomCard';

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
    equipment?: string[];
    imageUrl?: string;
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
    equipment: apiRoom.equipment || [], // Use backend equipment if available
    imageUrl: apiRoom.imageUrl || 'https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&q=80&w=1000', // Default image
    isAvailable: apiRoom.status === 'AVAILABLE'
});

export const roomApi = {
    getAllRooms: async (filter?: RoomFilter): Promise<Room[]> => { 
        const params = new URLSearchParams();
        
        if (filter?.capacity) params.append('capacity', filter.capacity.toString());
        // Add other filters as needed

        // Use real backend search endpoint
        // Backend default page size might be 20. We use 100 to get "all".
        try {
            const response = await client.get<ApiResponse<PageResponse<RoomResponse>>>('/rooms/search?size=100');
            
            // Check if response.data.content exists (PageResponse structure)
            if (response.data && response.data.content) {
                 return response.data.content.map(mapToRoom);
            }
            return [];
        } catch (error) {
            console.error("Failed to fetch rooms from backend", error);
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
