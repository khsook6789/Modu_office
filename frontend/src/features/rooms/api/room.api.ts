import { client } from '../../../api/client';
import { type Room } from '../RoomCard';

// Backend Room Response Structure
export interface OfficeRoomResponse {
    id: number;
    officeId: number;
    name: string;
    roomCode: string;
    floor: number;
    status: 'AVAILABLE' | 'RESERVED' | 'MAINTENANCE';
    capacity: number;
    category: 'MEETING_ROOM' | 'CONFERENCE_HALL' | 'FOCUS_ROOM' | 'STUDIO';
    imageUrl?: string;
}

// LocalStorage Keys
const ROOM_STORAGE_KEY = 'modu_rooms';

// Initial Seed Data
const INITIAL_ROOMS: OfficeRoomResponse[] = [
    { id: 101, officeId: 1, name: 'Meeting Room A', roomCode: '201', floor: 2, status: 'AVAILABLE', capacity: 4, category: 'MEETING_ROOM', imageUrl: 'https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&q=80&w=1000' },
    { id: 102, officeId: 1, name: 'Conference Hall', roomCode: '301', floor: 3, status: 'RESERVED', capacity: 20, category: 'CONFERENCE_HALL', imageUrl: 'https://images.unsplash.com/photo-1497366139122-de4747d96aed?auto=format&fit=crop&q=80&w=1000' },
    { id: 201, officeId: 2, name: 'Focus Room 1', roomCode: '101', floor: 1, status: 'AVAILABLE', capacity: 1, category: 'FOCUS_ROOM', imageUrl: 'https://images.unsplash.com/photo-1596524430615-b46475ddff6e?auto=format&fit=crop&q=80&w=1000' },
];

const getStoredRooms = (): OfficeRoomResponse[] => {
    const stored = localStorage.getItem(ROOM_STORAGE_KEY);
    if (!stored) {
        localStorage.setItem(ROOM_STORAGE_KEY, JSON.stringify(INITIAL_ROOMS));
        return INITIAL_ROOMS;
    }
    return JSON.parse(stored);
};

export const roomApi = {
    getAllRooms: () => {
        return new Promise<OfficeRoomResponse[]>((resolve) => {
            setTimeout(() => {
                resolve(getStoredRooms());
            }, 300);
        });
    },
    getRoomsByOffice: (officeId: number) => {
        return new Promise<OfficeRoomResponse[]>((resolve) => {
            setTimeout(() => {
                const rooms = getStoredRooms();
                resolve(rooms.filter(r => r.officeId === officeId));
            }, 300);
        });
    },
    // Mock updateRoom (missing in original interface but needed for edit)
    getRoomById: (roomId: number) => {
        return new Promise<OfficeRoomResponse>((resolve, reject) => {
            setTimeout(() => {
                const rooms = getStoredRooms();
                const room = rooms.find(r => r.id === roomId);
                if (room) resolve(room);
                else reject(new Error('Room not found'));
            }, 300);
        });
    },
    createRoom: (officeId: number, data: any) => {
        return new Promise<OfficeRoomResponse>((resolve) => {
            setTimeout(() => {
                const rooms = getStoredRooms();
                const newId = rooms.length > 0 ? Math.max(...rooms.map(r => r.id)) + 1 : 101;
                // Ensure officeId is set
                const newRoom = {
                    id: newId,
                    officeId,
                    status: 'AVAILABLE', // Default status
                    ...data
                };
                localStorage.setItem(ROOM_STORAGE_KEY, JSON.stringify([...rooms, newRoom]));
                resolve(newRoom);
            }, 500);
        });
    },
    updateRoom: (roomId: number, data: any) => {
        return new Promise<OfficeRoomResponse>((resolve, reject) => {
            setTimeout(() => {
                const rooms = getStoredRooms();
                const index = rooms.findIndex(r => r.id === roomId);
                if (index !== -1) {
                    const updatedRoom = { ...rooms[index], ...data };
                    rooms[index] = updatedRoom;
                    localStorage.setItem(ROOM_STORAGE_KEY, JSON.stringify(rooms));
                    resolve(updatedRoom);
                } else {
                    reject(new Error('Room not found'));
                }
            }, 500);
        });
    },
    deleteRoom: (roomId: number) => {
        return new Promise<void>((resolve, reject) => {
            setTimeout(() => {
                const rooms = getStoredRooms();
                const newRooms = rooms.filter(r => r.id !== roomId);
                if (rooms.length !== newRooms.length) {
                    localStorage.setItem(ROOM_STORAGE_KEY, JSON.stringify(newRooms));
                    resolve();
                } else {
                    reject(new Error('Room not found'));
                }
            }, 500);
        });
    }
};

// Helper to map API response to Frontend Room type
export const mapToRoom = (apiRoom: OfficeRoomResponse): Room => ({
    id: apiRoom.id.toString(),
    name: apiRoom.name,
    location: `${apiRoom.floor}F - ${apiRoom.roomCode}`, // Format location
    capacity: apiRoom.capacity,
    equipment: [], // Backend doesn't have equipment yet
    imageUrl: apiRoom.imageUrl || 'https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&q=80&w=1000', // Default image
    isAvailable: apiRoom.status === 'AVAILABLE'
});
