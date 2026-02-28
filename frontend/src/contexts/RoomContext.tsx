import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { type Room } from '../features/rooms/RoomCard';
import { roomApi } from '../features/rooms/api/room.api';



interface RoomContextType {
    rooms: Room[];
    addRoom: (officeId: number, room: Omit<Room, 'id' | 'isAvailable' | 'officeId'>) => void;
    deleteRoom: (id: string) => void;
}

const RoomContext = createContext<RoomContextType | undefined>(undefined);

export function RoomProvider({ children }: { children: ReactNode }) {
    const [rooms, setRooms] = useState<Room[]>([]);

    useEffect(() => {
        const fetchRooms = async () => {
            try {
                // Fetch from API (which uses its own localStorage/mock)
                const apiRooms = await roomApi.getAllRooms();
                // Map API response to UI model
                // const mappedRooms = apiRooms.map(mapToRoom); // Already mapped in API
                setRooms(apiRooms);
            } catch (err) {
                console.error("Failed to load rooms", err);
            }
        };
        fetchRooms();
    }, []);

    // rooms 변경 시 localStorage 저장을 제거함
    // (초기 mount 시 빈 배열로 덮어쓰는 버그 방지 - API fetch가 비동기라 완료 전에 저장됨)



    const addRoom = async (officeId: number, roomData: Omit<Room, 'id' | 'isAvailable' | 'officeId'>) => {
        try {
            // 1. Map frontend input to backend request
            const payload = {
                name: roomData.name,
                roomCode: roomData.location.slice(0, 50), 
                floor: 1, 
                capacity: roomData.capacity,
                category: 'MEETING_ROOM',
                status: 'AVAILABLE',
                price: (roomData as any).price ?? 0,
                facilityIds: [] 
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
                rating: (roomData as any).rating
            };

            // 4. Update state
            setRooms(prev => [...prev, newRoom]);
            alert('회의실이 성공적으로 추가되었습니다! (DB 저장 완료)');
        } catch (error: any) {
            console.error("Failed to create room", error);
            const msg = error.response?.data?.message || error.message || "서버 오류가 발생했습니다.";
            alert(`회의실 추가 실패: ${msg}`);
        }
    };

    const deleteRoom = async (id: string) => {
        try {
            await roomApi.deleteRoom(Number(id));
            setRooms(prev => prev.filter(room => room.id !== id));
        } catch (error: any) {
            console.error("Failed to delete room", error);
            const msg = error.response?.data?.message || error.message || "서버 오류가 발생했습니다.";
            alert(`회의실 삭제 실패: ${msg}`);
        }
    };

    return (
        <RoomContext.Provider value={{ rooms, addRoom, deleteRoom }}>
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
