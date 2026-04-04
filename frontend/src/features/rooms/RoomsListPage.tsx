import { useState, useEffect, useRef } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useRooms } from '../../contexts/RoomContext';
import { OfficeProvider, useOfficeContext } from '../../contexts/OfficeContext';
import { OfficeSelectorDropdown } from '../../components/OfficeSelectorDropdown';
import { Autocomplete, useJsApiLoader } from '@react-google-maps/api';
import RoomCard from './RoomCard';
import Input from '../../components/Input';
import { roomApi, type FacilityResponse } from './api/room.api';
import { useToast } from '../../components/Toast';

const LIBRARIES: ('places')[] = ['places'];

import './RoomsListPage.css';

// Mock Data removed - using properties from RoomContext


// Wrapper component that provides Office context
export default function RoomsListPage() {
    return (
        <OfficeProvider>
            <RoomsListPageContent />
        </OfficeProvider>
    );
}

// Main component that uses Office context
function RoomsListPageContent() {
    const { user } = useAuth();
    const { rooms, addRoom, deleteRoom } = useRooms();
    const { selectedOfficeId, selectedOffice, offices, createOffice, deleteOffice } = useOfficeContext();
    const toast = useToast();
    // 검색 필터
    const [keyword, setKeyword] = useState('');
    const [minCapacity, setMinCapacity] = useState('');
    const [minPrice, setMinPrice] = useState('');
    const [maxPrice, setMaxPrice] = useState('');
    const [selectedFacilityNames, setSelectedFacilityNames] = useState<string[]>([]);
    const [onlyAvailable, setOnlyAvailable] = useState(false);
    const [searchedRooms, setSearchedRooms] = useState<typeof rooms | null>(null);
    const [searching, setSearching] = useState(false);

    const isFiltered = !!(keyword || minCapacity || minPrice || maxPrice || selectedFacilityNames.length || onlyAvailable);

    const handleSearch = async () => {
        setSearching(true);
        try {
            const results = await roomApi.searchRooms({
                keyword: keyword || undefined,
                minCapacity: minCapacity ? Number(minCapacity) : undefined,
                minPrice: minPrice ? Number(minPrice) : undefined,
                maxPrice: maxPrice ? Number(maxPrice) : undefined,
                facilityNames: selectedFacilityNames.length ? selectedFacilityNames : undefined,
            });
            const filtered = onlyAvailable ? results.filter(r => r.isAvailable) : results;
            const byOffice = selectedOfficeId ? filtered.filter(r => r.officeId === selectedOfficeId) : filtered;
            setSearchedRooms(byOffice);
        } catch {
        } finally {
            setSearching(false);
        }
    };

    const handleReset = () => {
        setKeyword('');
        setMinCapacity('');
        setMinPrice('');
        setMaxPrice('');
        setSelectedFacilityNames([]);
        setOnlyAvailable(false);
        setSearchedRooms(null);
    };

    const toggleFacility = (name: string) => {
        setSelectedFacilityNames(prev =>
            prev.includes(name) ? prev.filter(n => n !== name) : [...prev, name]
        );
    };

    // Modal State
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isOfficeModalOpen, setIsOfficeModalOpen] = useState(false);
    
    // Facility State
    const [facilities, setFacilities] = useState<FacilityResponse[]>([]);
    const [selectedFacilities, setSelectedFacilities] = useState<number[]>([]);

    useEffect(() => {
        const loadFacilities = async () => {
            const data = await roomApi.getActiveFacilities();
            setFacilities(data);
        };
        loadFacilities();
    }, []);

    const [newRoom, setNewRoom] = useState({
        name: '',
        description: '',
        floor: 1,
        roomCode: '',
        capacity: 4,
        imageUrl: '',
        price: 0,
        bufferTime: 0
    });

    const [newOfficeData, setNewOfficeData] = useState({
        name: '',
        description: '',
        location: '',
        openTime: '09:00',
        closeTime: '18:00'
    });
    const [officeLat, setOfficeLat] = useState(37.5665);
    const [officeLng, setOfficeLng] = useState(126.9780);
    const officeAutocompleteRef = useRef<google.maps.places.Autocomplete | null>(null);

    const { isLoaded: mapsLoaded } = useJsApiLoader({
        googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY || '',
        libraries: LIBRARIES,
    });

    const handleOfficeAddressChange = () => {
        const place = officeAutocompleteRef.current?.getPlace();
        if (!place?.geometry?.location) return;
        setOfficeLat(place.geometry.location.lat());
        setOfficeLng(place.geometry.location.lng());
        setNewOfficeData(prev => ({ ...prev, location: place.formatted_address || place.name || '' }));
    };

    // 검색 결과 있으면 검색 결과, 없으면 전체 (오피스 필터 적용)
    const displayRooms = searchedRooms ?? rooms.filter(room => {
        const matchesOffice = !selectedOfficeId || room.officeId === selectedOfficeId;
        const matchesAvailability = onlyAvailable ? room.isAvailable : true;
        return matchesOffice && matchesAvailability;
    });

    const handleCreateOffice = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await createOffice({
                name: newOfficeData.name,
                description: newOfficeData.description,
                location: newOfficeData.location,
                latitude: officeLat,
                longitude: officeLng,
                openTime: newOfficeData.openTime || '09:00',
                closeTime: newOfficeData.closeTime || '18:00',
                openDays: [1, 2, 3, 4, 5]
            });
            setIsOfficeModalOpen(false);
            setNewOfficeData({ name: '', description: '', location: '', openTime: '09:00', closeTime: '18:00' });
            toast.success('오피스가 성공적으로 생성되었습니다!');
        } catch (err: any) {
            console.error('Office creation error:', err);
            toast.error('오피스 생성에 실패했습니다: ' + (err?.message || '알 수 없는 오류'));
        }
    };

    const handleAddRoom = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!selectedOfficeId) {
            toast.warning('오피스를 먼저 선택해주세요.');
            return;
        }

        try {
            await addRoom(selectedOfficeId, {
                name: newRoom.name,
                description: newRoom.description,
                floor: Number(newRoom.floor),
                roomCode: newRoom.roomCode,
                capacity: Number(newRoom.capacity),
                facilityIds: selectedFacilities,
                imageUrl: newRoom.imageUrl || undefined,
                price: Number(newRoom.price) || 0,
                bufferTime: Number(newRoom.bufferTime) || 0
            });
            toast.success('회의실이 성공적으로 추가되었습니다!');
            setIsModalOpen(false);
            setNewRoom({ name: '', description: '', floor: 1, roomCode: '', capacity: 4, imageUrl: '', price: 0, bufferTime: 0 });
            setSelectedFacilities([]);
        } catch (err: any) {
            toast.error('회의실 추가 실패: ' + (err?.message || '서버 오류'));
        }
    };

    return (
        <div className="rooms-page">
            <div className="rooms-page-header">
                <div className="rooms-title-area">
                    <h1 className="text-3xl font-bold text-gradient">공간 예약</h1>
                    <p className="rooms-subtitle">최고의 회의 경험을 위한 완벽한 공간을 찾아보세요</p>
                </div>
                <div className="rooms-actions">
                    {(user?.role === 'ADMIN' || user?.role === 'MANAGER') && (
                        <>
                            <button 
                                className="btn-action-outline"
                                onClick={() => setIsOfficeModalOpen(true)}
                            >
                                오피스 관리
                            </button>
                            <button 
                                className="btn-action-primary"
                                onClick={() => setIsModalOpen(true)}
                                disabled={!selectedOfficeId}
                                title={!selectedOfficeId ? '먼저 오피스를 선택해주세요' : ''}
                            >
                                + 회의실 추가
                            </button>
                        </>
                    )}
                </div>
            </div>

            {/* Office Selector Panel */}
            <div className="office-select-panel">
                <div className="office-select-header">
                    <div style={{ flex: 1 }}>
                        <OfficeSelectorDropdown />
                    </div>
                    {offices.length === 0 && (user?.role === 'MANAGER' || user?.role === 'ADMIN') && (
                        <button className="btn-action-primary" onClick={() => setIsOfficeModalOpen(true)}>
                            첫 오피스 만들기
                        </button>
                    )}
                </div>
                
                {selectedOffice && (
                    <div className="office-select-info">
                        <div className="office-info-text">
                            <span>📍 {selectedOffice.location}</span>
                            <span>🕒 {selectedOffice.openTime} - {selectedOffice.closeTime}</span>
                            {selectedOffice.description && (
                                <span style={{ color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>
                                    {selectedOffice.description}
                                </span>
                            )}
                        </div>
                        {(user?.role === 'MANAGER' || user?.role === 'ADMIN') && (
                            <button
                                className="btn-action-danger"
                                onClick={async () => {
                                    if (confirm(`"${selectedOffice.name}" 오피스를 삭제하시겠습니까?\n⚠️ 소속 회의실이 없어야 삭제 가능합니다.`)) {
                                        try {
                                            await deleteOffice(selectedOffice.id);
                                            toast.success('오피스가 삭제되었습니다.');
                                        } catch (err: any) {
                                            toast.error('오피스 삭제 실패: ' + (err?.message || '서버 오류'));
                                        }
                                    }
                                }}
                            >
                                오피스 삭제
                            </button>
                        )}
                    </div>
                )}
            </div>

            {/* 검색 필터 패널 */}
            <div className="search-filter-panel">
                {/* 키워드 + 검색 버튼 */}
                <div className="search-filter-row">
                    <input
                        type="text"
                        className="search-filter-input"
                        placeholder="회의실 이름으로 검색..."
                        value={keyword}
                        onChange={e => setKeyword(e.target.value)}
                        onKeyDown={e => e.key === 'Enter' && handleSearch()}
                    />
                    <button className="btn-action-primary" onClick={handleSearch} disabled={searching}>
                        {searching ? '검색 중...' : '검색'}
                    </button>
                    {isFiltered && (
                        <button className="btn-action-outline" onClick={handleReset}>초기화</button>
                    )}
                </div>

                {/* 상세 필터 */}
                <div className="search-filter-details">
                    <div className="search-filter-group">
                        <label className="search-filter-label">최소 인원</label>
                        <input
                            type="number"
                            className="search-filter-number"
                            placeholder="명"
                            min={1}
                            value={minCapacity}
                            onChange={e => setMinCapacity(e.target.value)}
                        />
                    </div>
                    <div className="search-filter-group">
                        <label className="search-filter-label">가격 (원/시간)</label>
                        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                            <input
                                type="number"
                                className="search-filter-number"
                                placeholder="최소"
                                min={0}
                                step={1000}
                                value={minPrice}
                                onChange={e => setMinPrice(e.target.value)}
                            />
                            <span style={{ color: 'var(--color-text-muted)', fontSize: '0.875rem' }}>~</span>
                            <input
                                type="number"
                                className="search-filter-number"
                                placeholder="최대"
                                min={0}
                                step={1000}
                                value={maxPrice}
                                onChange={e => setMaxPrice(e.target.value)}
                            />
                        </div>
                    </div>
                    <div className="search-filter-group">
                        <label className="search-filter-label">예약 가능만</label>
                        <label className="search-filter-toggle">
                            <input
                                type="checkbox"
                                checked={onlyAvailable}
                                onChange={e => setOnlyAvailable(e.target.checked)}
                                style={{ accentColor: 'var(--color-primary)' }}
                            />
                            <span>예약 가능</span>
                        </label>
                    </div>
                </div>

                {/* 시설 필터 */}
                {facilities.length > 0 && (
                    <div className="search-filter-facilities">
                        <span className="search-filter-label">시설/장비</span>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginTop: '0.5rem' }}>
                            {facilities.map(f => (
                                <button
                                    key={f.id}
                                    type="button"
                                    onClick={() => toggleFacility(f.facilityName)}
                                    className="search-facility-tag"
                                    style={{
                                        background: selectedFacilityNames.includes(f.facilityName) ? 'var(--color-primary)' : '#f1f5f9',
                                        color: selectedFacilityNames.includes(f.facilityName) ? '#fff' : 'var(--color-text-sub)',
                                        border: selectedFacilityNames.includes(f.facilityName) ? 'none' : '1px solid #e2e8f0',
                                    }}
                                >
                                    {f.facilityName}
                                </button>
                            ))}
                        </div>
                    </div>
                )}

                {/* 결과 수 */}
                {isFiltered && (
                    <p className="search-result-count">
                        {searching ? '검색 중...' : `${displayRooms.length}개의 회의실`}
                    </p>
                )}
            </div>

            {/* Rooms Grid */}
            {displayRooms.length > 0 ? (
                <div className="rooms-grid">
                    {displayRooms.map(room => (
                        <RoomCard key={room.id} room={room} isManager={user?.role === 'MANAGER' || user?.role === 'ADMIN'} onDelete={deleteRoom} />
                    ))}
                </div>
            ) : (
                <div className="empty-state">
                    <div className="empty-state-icon">🏢</div>
                    <p className="empty-state-text">
                        {selectedOfficeId ? '이 오피스에는 아직 등록된 회의실이 없습니다.' : '등록된 회의실이 없습니다.'}
                    </p>
                    {(user?.role === 'MANAGER' || user?.role === 'ADMIN') && (
                        <p style={{ marginTop: '0.5rem', fontSize: '0.9rem' }}>관리자 기능으로 새로운 회의실을 추가해보세요.</p>
                    )}
                </div>
            )}

            {/* New Office Modal */}
            {isOfficeModalOpen && (
                <div className="modal-overlay" style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    backgroundColor: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center',
                    zIndex: 1000, backdropFilter: 'blur(8px)'
                }}>
                    <div className="card" style={{ width: '100%', maxWidth: '400px', backgroundColor: 'var(--color-bg-card)' }}>
                        <h2 className="text-xl font-bold mb-md text-gradient">새 오피스(지점) 등록</h2>
                        <form onSubmit={handleCreateOffice}>
                            <Input label="오피스 이름" value={newOfficeData.name} onChange={e => setNewOfficeData({...newOfficeData, name: e.target.value})} required fullWidth />
                            <div className="form-group mb-sm">
                                <label className="block text-sm font-medium mb-xs">오피스 설명 <span style={{color:'var(--color-error)'}}>*</span></label>
                                <textarea
                                    className="input-field w-full"
                                    style={{ minHeight: '80px', resize: 'vertical' }}
                                    value={newOfficeData.description}
                                    onChange={e => setNewOfficeData({...newOfficeData, description: e.target.value})}
                                    placeholder="오피스 설명을 5자 이상 입력하세요."
                                    required
                                    minLength={5}
                                />
                            </div>
                            {mapsLoaded ? (
                                <div className="form-group mb-sm">
                                    <label className="block text-sm font-medium mb-xs">위치 (주소) <span style={{color:'var(--color-error)'}}>*</span></label>
                                    <Autocomplete
                                        onLoad={ac => (officeAutocompleteRef.current = ac)}
                                        onPlaceChanged={handleOfficeAddressChange}
                                        options={{ componentRestrictions: { country: 'kr' } }}
                                    >
                                        <input
                                            type="text"
                                            className="input-field w-full"
                                            value={newOfficeData.location}
                                            onChange={e => setNewOfficeData({...newOfficeData, location: e.target.value})}
                                            placeholder="주소를 검색하세요..."
                                            required
                                        />
                                    </Autocomplete>
                                </div>
                            ) : (
                                <Input label="위치 (주소)" value={newOfficeData.location} onChange={e => setNewOfficeData({...newOfficeData, location: e.target.value})} required fullWidth />
                            )}
                            
                            <div className="flex gap-sm mt-lg justify-end">
                                <button type="button" className="btn btn-secondary" onClick={() => setIsOfficeModalOpen(false)}>취소</button>
                                <button type="submit" className="btn btn-primary">생성하기</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Simple Modal for Adding Room */}
            {isModalOpen && (
                <div className="modal-overlay" style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    backgroundColor: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center',
                    zIndex: 1000, backdropFilter: 'blur(8px)'
                }}>
                    <div className="card" style={{ width: '100%', maxWidth: '500px', maxHeight: '90vh', overflowY: 'auto', backgroundColor: 'var(--color-bg-card)' }}>
                        <h2 className="text-xl font-bold mb-md text-gradient">새 회의실 추가</h2>
                        {selectedOffice && (
                            <p style={{ marginBottom: '15px', padding: '10px', backgroundColor: 'rgba(217, 190, 158, 0.1)', border: '1px solid rgba(217, 190, 158, 0.2)', borderRadius: 'var(--radius-md)', color: 'var(--color-primary)' }}>
                                <strong>{selectedOffice.name}</strong>에 회의실을 추가합니다
                            </p>
                        )}
                        <form onSubmit={handleAddRoom}>
                            <Input label="회의실 이름" value={newRoom.name} onChange={e => setNewRoom({...newRoom, name: e.target.value})} required fullWidth />
                            <div className="form-group mb-sm">
                                <label className="block text-sm font-medium mb-xs">회의실 설명 <span style={{color:'var(--color-error)'}}>*</span></label>
                                <textarea
                                    className="input-field w-full"
                                    style={{ minHeight: '80px', resize: 'vertical' }}
                                    value={newRoom.description}
                                    onChange={e => setNewRoom({...newRoom, description: e.target.value})}
                                    placeholder="회의실 설명을 5자 이상 입력하세요."
                                    required
                                    minLength={5}
                                />
                            </div>
                            <div className="flex gap-sm w-full mb-sm" style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
                                <div style={{ flex: 1 }}>
                                    <Input label="해당 층 (-1, 1, 2...)" type="number" value={newRoom.floor} onChange={e => setNewRoom({...newRoom, floor: Number(e.target.value)})} required fullWidth />
                                </div>
                                <div style={{ flex: 2 }}>
                                    <Input label="호수 (예: 301호)" value={newRoom.roomCode} onChange={e => setNewRoom({...newRoom, roomCode: e.target.value})} required fullWidth />
                                </div>
                            </div>
                            <div className="flex gap-sm w-full mb-sm" style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
                                <div style={{ flex: 1 }}>
                                    <Input label="수용 인원" type="number" value={newRoom.capacity} onChange={e => setNewRoom({...newRoom, capacity: Number(e.target.value)})} required fullWidth />
                                </div>
                                <div style={{ flex: 1 }}>
                                    <Input label="시간당 가격(원)" type="number" value={newRoom.price} onChange={e => setNewRoom({...newRoom, price: Number(e.target.value)})} required fullWidth />
                                </div>
                                <div style={{ flex: 1 }}>
                                    <Input label="정비 시간(분)" type="number" value={newRoom.bufferTime} onChange={e => setNewRoom({...newRoom, bufferTime: Number(e.target.value)})} required fullWidth />
                                </div>
                            </div>
                            <div className="form-group mb-sm">
                                <label className="block text-sm font-medium mb-xs">시설/장비 선택</label>
                                {facilities.length > 0 ? (
                                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                                        {facilities.map(facility => {
                                            const isSelected = selectedFacilities.includes(facility.id);
                                            return (
                                                <button
                                                    key={facility.id}
                                                    type="button"
                                                    onClick={() => {
                                                        setSelectedFacilities(prev => 
                                                            prev.includes(facility.id) 
                                                                ? prev.filter(id => id !== facility.id)
                                                                : [...prev, facility.id]
                                                        )
                                                    }}
                                                    className="btn-action-outline"
                                                    style={{ 
                                                        padding: '0.4rem 0.8rem', 
                                                        borderRadius: '20px',
                                                        backgroundColor: isSelected ? 'var(--color-primary)' : 'transparent',
                                                        color: isSelected ? '#fff' : 'inherit',
                                                        borderColor: isSelected ? 'var(--color-primary)' : 'var(--color-border)',
                                                        fontSize: '0.85rem'
                                                    }}
                                                >
                                                    {facility.facilityName}
                                                </button>
                                            );
                                        })}
                                    </div>
                                ) : (
                                    <div style={{ padding: '0.5rem', backgroundColor: 'rgba(255,255,255,0.05)', borderRadius: '8px', fontSize: '0.9rem', color: '#94a3b8' }}>
                                        등록된 시설/장비가 없습니다.
                                    </div>
                                )}
                            </div>
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
