import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { officeApi, type Office } from '../rooms/api/office.api';
import { roomApi } from '../rooms/api/room.api';

export default function OperatorDashboard() {
    const navigate = useNavigate();
    const [offices, setOffices] = useState<Office[]>([]);
    const [loading, setLoading] = useState(true);
    const [stats, setStats] = useState({
        totalRooms: 0,
        activeOffices: 0,
        totalCapacity: 0
    });

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        try {
            setLoading(true);
            const officesData = await officeApi.getMyOffices();
            const roomsData = await roomApi.getAllRooms();

            setOffices(officesData);

            // Calculate stats
            setStats({
                totalRooms: roomsData.length,
                activeOffices: officesData.length,
                totalCapacity: roomsData.reduce((acc, room) => acc + room.capacity, 0)
            });

        } catch (error) {
            console.error("Failed to load dashboard data", error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container mx-auto p-md">
            <div className="flex justify-between items-center mb-lg">
                <h1 className="text-3xl font-bold">운영자 대시보드</h1>
                <button
                    className="btn btn-primary"
                    onClick={() => navigate('/office/new')}
                >
                    + 새 오피스 등록
                </button>
            </div>

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-md mb-xl">
                <div className="card bg-white shadow-sm p-lg">
                    <h3 className="text-muted text-sm font-bold uppercase mb-xs">운영 중인 오피스</h3>
                    <p className="text-3xl font-bold text-primary">{stats.activeOffices}개</p>
                </div>
                <div className="card bg-white shadow-sm p-lg">
                    <h3 className="text-muted text-sm font-bold uppercase mb-xs">총 회의실 수</h3>
                    <p className="text-3xl font-bold text-accent">{stats.totalRooms}개</p>
                </div>
                <div className="card bg-white shadow-sm p-lg">
                    <h3 className="text-muted text-sm font-bold uppercase mb-xs">총 수용 인원</h3>
                    <p className="text-3xl font-bold">{stats.totalCapacity}명</p>
                </div>
            </div>

            {/* Office List */}
            <h2 className="text-xl font-bold mb-md">내 오피스 목록</h2>
            {loading ? (
                <div className="text-center p-xl">로딩 중...</div>
            ) : offices.length === 0 ? (
                <div className="text-center p-xl bg-gray-50 rounded text-muted">등록된 오피스가 없습니다.</div>
            ) : (
                <div className="grid grid-cols-1 gap-md">
                    {offices.map((office) => (
                        <div key={office.id} className="card bg-white shadow-sm p-md flex justify-between items-center">
                            <div>
                                <h3 className="text-lg font-bold mb-xs">{office.name}</h3>
                                <p className="text-muted text-sm">{office.location}</p>
                                <p className="text-xs text-gray-500 mt-1">운영시간: {office.openTime} ~ {office.closeTime}</p>
                            </div>
                            <div className="flex items-center gap-md">
                                <button
                                    className="btn btn-outline btn-sm"
                                    onClick={() => navigate(`/office/${office.id}/manage`)}
                                >
                                    관리
                                </button>
                                <button
                                    className="btn btn-ghost btn-sm text-muted"
                                    onClick={() => navigate(`/office/new?id=${office.id}`)}
                                >
                                    수정
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
