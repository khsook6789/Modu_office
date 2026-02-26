import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { roomApi, type RoomResponse } from './api/room.api';
import { officeApi } from './api/office.api';

export default function RoomManagementPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [rooms, setRooms] = useState<RoomResponse[]>([]);
    const [officeName, setOfficeName] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!id) return;

        loadData();
    }, [id]);

    const loadData = () => {
        setLoading(true);
        // Parallel data fetching
        Promise.all([
            officeApi.getOfficeById(id!),
            roomApi.getRoomsByOffice(Number(id))
        ]).then(([officeData, roomData]) => {
            setOfficeName(officeData.name);
            setRooms(roomData);
        }).catch(err => {
            console.error("Failed to load data", err);
            alert('오피스 정보를 불러오는데 실패했습니다.');
            navigate('/operator');
        }).finally(() => {
            setLoading(false);
        });
    };

    const handleDelete = async (roomId: number) => {
        if (window.confirm('정말로 이 회의실을 삭제하시겠습니까?')) {
            try {
                await roomApi.deleteRoom(roomId);
                setRooms(prev => prev.filter(r => r.id !== roomId));
                alert('삭제되었습니다.');
            } catch (error) {
                console.error("Failed to delete room", error);
                alert('삭제에 실패했습니다.');
            }
        }
    };

    return (
        <div className="container mx-auto p-md">
            <div className="flex justify-between items-center mb-lg">
                <div>
                    <h1 className="text-2xl font-bold">{officeName} - 회의실 관리</h1>
                    <p className="text-muted text-sm">오피스 내 회의실을 관리합니다.</p>
                </div>
                <button
                    onClick={() => navigate(`/office/${id}/rooms/new`)}
                    className="btn btn-primary"
                >
                    + 회의실 추가
                </button>
            </div>

            {loading ? (
                <div className="text-center p-xl">로딩 중...</div>
            ) : (
                <div className="bg-white rounded-lg shadow-sm overflow-hidden">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-gray-50 border-b text-sm text-muted uppercase">
                                <th className="p-md font-medium">이름</th>
                                <th className="p-md font-medium">위치 (층-호수)</th>
                                <th className="p-md font-medium">유형</th>
                                <th className="p-md font-medium">수용인원</th>
                                <th className="p-md font-medium">등록일</th>
                                <th className="p-md font-medium text-center">관리</th>
                            </tr>
                        </thead>
                        <tbody>
                            {rooms.length === 0 ? (
                                <tr>
                                    <td colSpan={6} className="p-xl text-center text-muted">등록된 회의실이 없습니다.</td>
                                </tr>
                            ) : (
                                rooms.map((room) => (
                                    <tr key={room.id} className="border-b hover:bg-gray-50">
                                        <td className="p-md font-medium">{room.name}</td>
                                        <td className="p-md">{room.floor}층 {room.roomCode}호</td>
                                        <td className="p-md text-sm">
                                            <span className="bg-gray-100 px-2 py-1 rounded text-xs">{room.category}</span>
                                        </td>
                                        <td className="p-md">{room.capacity}명</td>
                                        <td className="p-md text-muted text-sm">2023-10-20</td>
                                        <td className="p-md text-center">
                                            <div className="flex justify-center gap-2">
                                                <button
                                                    onClick={() => navigate(`/rooms/${room.id}/edit`)}
                                                    className="btn btn-sm btn-outline px-3 py-1"
                                                >
                                                    수정
                                                </button>
                                                <button
                                                    onClick={() => handleDelete(room.id)}
                                                    className="btn btn-sm text-red-500 hover:bg-red-50 px-3 py-1 rounded"
                                                >
                                                    삭제
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}
