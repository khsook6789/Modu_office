import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { type Room } from '../features/rooms/RoomCard';
import { roomApi } from '../features/rooms/api/room.api';
import { useAuth } from './AuthContext';

interface RoomContextType {
    rooms: Room[];
    addRoom: (officeId: number, room: any) => void;
    deleteRoom: (id: string) => void;
    refreshRooms: () => Promise<void>;
}

const RoomContext = createContext<RoomContextType | undefined>(undefined);

export function RoomProvider({ children }: { children: ReactNode }) {
    const { user } = useAuth();
    const [rooms, setRooms] = useState<Room[]>([]);

    const fetchRooms = async () => {
        const token = localStorage.getItem('token');
        if (!token) {
            setRooms([]); // 미인증 상태면 리스트 초기화 후 생략
            return;
        }

        try {
            const apiRooms = await roomApi.getAllRooms();
            setRooms(apiRooms);
        } catch (err) {
            console.error("Failed to load rooms", err);
        }
    };

    useEffect(() => {
        fetchRooms();
    }, [user?.id]); // User가 로그인/로그아웃할 때마다 방 목록 재조회

    // rooms 변경 시 localStorage 저장을 제거함
    // (초기 mount 시 빈 배열로 덮어쓰는 버그 방지 - API fetch가 비동기라 완료 전에 저장됨)
    const addRoom = async (officeId: number, roomData: any) => {
        try {
            // 1. Map frontend input to backend request
            const payload = {
                name: roomData.name,
                description: roomData.description,
                roomCode: roomData.roomCode, 
                floor: roomData.floor, 
                capacity: roomData.capacity,
                category: 'MEETING_ROOM',
                status: 'AVAILABLE',
                price: roomData.price ?? 0,
                bufferTime: roomData.bufferTime ?? 0,
                facilityIds: roomData.facilityIds || [],
                images: roomData.imageUrl ? [{ imageUrl: roomData.imageUrl, displayOrder: 1 }] : []
            };

            // 2. Call API with provided Office ID
            const createdRoomDTO = await roomApi.createRoom(officeId, payload);

            // 3. Map backend response back to frontend model
            const newRoom: Room = {
                id: createdRoomDTO.id.toString(),
                officeId: officeId,  // Include officeId for filtering
                name: createdRoomDTO.name,
                location: `${createdRoomDTO.floor}F - ${createdRoomDTO.roomCode}`,
                capacity: createdRoomDTO.capacity,
                equipment: roomData.equipment || [],
                imageUrl: roomData.imageUrl,
                isAvailable: createdRoomDTO.status === 'AVAILABLE',
                rating: roomData.rating,
                floor: createdRoomDTO.floor,
                roomCode: createdRoomDTO.roomCode,
                description: createdRoomDTO.description,
                price: createdRoomDTO.price,
                bufferTime: createdRoomDTO.bufferTime
            };

            // 4. Update state
            setRooms(prev => [...prev, newRoom]);
        } catch (error: any) {
            console.error("Failed to create room", error);
            throw error;
        }
    };

    const deleteRoom = async (id: string) => {
        try {
            await roomApi.deleteRoom(Number(id));
            setRooms(prev => prev.filter(room => room.id !== id));
        } catch (error: any) {
            console.error("Failed to delete room", error);
            throw error;
        }
    };

    return (
        <RoomContext.Provider value={{ rooms, addRoom, deleteRoom, refreshRooms: fetchRooms }}>
            {children}
        </RoomContext.Provider>
    );
}

export function useRooms() {
    const context = useContext(RoomContext);
    if (context === undefined) {
        throw new Error('useRooms must be used within a RoomProvider');
    }
    return context;
}
