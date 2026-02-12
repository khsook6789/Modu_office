import { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useRooms } from '../../contexts/RoomContext';
import RoomCard from './RoomCard';
import Input from '../../components/Input';
import { officeApi, type Office } from './api/office.api';
import OfficeMap from '../../components/Map/OfficeMap'; // Import the new component
import './RoomsListPage.css';

// Mock Data removed - using properties from RoomContext

export default function RoomsListPage() {
    const { user } = useAuth();
    const { rooms, addRoom } = useRooms();
    const [filter, setFilter] = useState<'ALL' | 'AVAILABLE'>('ALL');
    const [viewMode, setViewMode] = useState<'LIST' | 'MAP'>('LIST'); // New state for view mode
    const [offices, setOffices] = useState<Office[]>([]); // New state for offices

    useEffect(() => {
        // Fetch offices when component mounts
        officeApi.getAllOffices()
            .then(data => setOffices(data))
            .catch(err => console.error('Failed to fetch offices:', err));
    }, []);
    
    // Modal State
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [newRoom, setNewRoom] = useState({
        name: '',
        location: '',
        capacity: 4,
        equipment: '',
        imageUrl: ''
    });

    const filteredRooms = filter === 'ALL'
        ? rooms
        : rooms.filter(room => room.isAvailable);

    const handleAddRoom = (e: React.FormEvent) => {
        e.preventDefault();
        addRoom({
            name: newRoom.name,
            location: newRoom.location,
            capacity: Number(newRoom.capacity),
            equipment: newRoom.equipment.split(',').map(item => item.trim()).filter(Boolean),
            imageUrl: newRoom.imageUrl || undefined
        });
        setIsModalOpen(false);
        setNewRoom({ name: '', location: '', capacity: 4, equipment: '', imageUrl: '' });
    };

    return (
        <div className="rooms-page">
            <div className="rooms-page-header">
                <div>
                    <h1 className="text-3xl font-bold text-gradient">공간 예약</h1>
                    <p className="rooms-subtitle">회의에 적합한 공간을 찾아보세요</p>
                </div>
                <div style={{ display: 'flex', gap: '10px' }}>
                     <div className="view-toggle" style={{ display: 'flex', background: '#333', borderRadius: '8px', padding: '4px' }}>
                        <button 
                            className={`btn ${viewMode === 'LIST' ? 'btn-primary' : 'btn-ghost'}`}
                            onClick={() => setViewMode('LIST')}
                            style={{ padding: '6px 12px', fontSize: '14px' }}
                        >
                            목록
                        </button>
                        <button 
                            className={`btn ${viewMode === 'MAP' ? 'btn-primary' : 'btn-ghost'}`}
                            onClick={() => setViewMode('MAP')}
                            style={{ padding: '6px 12px', fontSize: '14px' }}
                        >
                            지도
                        </button>
                    </div>

                    {user?.role === 'ADMIN' && (
                        <button 
                            className="btn btn-primary"
                            onClick={() => setIsModalOpen(true)}
                        >
                            + 회의실 추가
                        </button>
                    )}
                </div>
            </div>

            {viewMode === 'LIST' ? (
                <>
                    <div className="filters-bar">
                        <button
                            className={`filter-btn ${filter === 'ALL' ? 'active' : ''}`}
                            onClick={() => setFilter('ALL')}
                        >
                            전체
                        </button>
                        <button
                            className={`filter-btn ${filter === 'AVAILABLE' ? 'active' : ''}`}
                            onClick={() => setFilter('AVAILABLE')}
                        >
                            예약 가능
                        </button>
                    </div>

                    <div className="rooms-grid">
                        {filteredRooms.map(room => (
                            <RoomCard key={room.id} room={room} />
                        ))}
                    </div>
                </>
            ) : (
                <div style={{ marginTop: '20px' }}>
                     {/* Pass fetched offices to the map */}
                    <OfficeMap offices={offices} />
                </div>
            )}

            {/* Simple Modal for Adding Room */}
            {isModalOpen && (
                <div className="modal-overlay" style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center',
                    zIndex: 1000, backdropFilter: 'blur(4px)'
                }}>
                    <div className="card" style={{ width: '100%', maxWidth: '500px', maxHeight: '90vh', overflowY: 'auto' }}>
                        <h2 className="text-xl font-bold mb-md">새 회의실 추가</h2>
                        <form onSubmit={handleAddRoom}>
                            <Input label="회의실 이름" value={newRoom.name} onChange={e => setNewRoom({...newRoom, name: e.target.value})} required fullWidth />
                            <Input label="위치" value={newRoom.location} onChange={e => setNewRoom({...newRoom, location: e.target.value})} required fullWidth />
                            <Input label="수용 인원" type="number" value={newRoom.capacity} onChange={e => setNewRoom({...newRoom, capacity: Number(e.target.value)})} required fullWidth />
                            <Input label="장비 (쉼표로 구분)" placeholder="TV, 화이트보드, 프로젝터" value={newRoom.equipment} onChange={e => setNewRoom({...newRoom, equipment: e.target.value})} fullWidth />
                            <Input label="이미지 URL (선택)" placeholder="https://..." value={newRoom.imageUrl} onChange={e => setNewRoom({...newRoom, imageUrl: e.target.value})} fullWidth />
                            
                            <div className="flex gap-sm mt-lg justify-end">
                                <button type="button" className="btn btn-secondary" onClick={() => setIsModalOpen(false)}>취소</button>
                                <button type="submit" className="btn btn-primary">추가하기</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
