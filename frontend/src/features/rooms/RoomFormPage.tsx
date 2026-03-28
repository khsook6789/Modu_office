import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Input from '../../components/Input';
import { roomApi } from './api/room.api';
import { useToast } from '../../components/Toast';

// Enum for Room Category
const CHECK_OPTIONS = [
    { id: 'PROJECTOR', label: '빔프로젝터' },
    { id: 'WHITEBOARD', label: '화이트보드' },
    { id: 'WIFI', label: '초고속 와이파이' },
    { id: 'MONITOR', label: '대형 모니터' },
    { id: 'MIC', label: '마이크/음향설비' },
];

export default function RoomFormPage() {
    const { officeId, roomId } = useParams();
    const navigate = useNavigate();
    const toast = useToast();
    const isEditMode = !!roomId;

    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [floor, setFloor] = useState('1');
    const [roomCode, setRoomCode] = useState('');
    const [capacity, setCapacity] = useState('4');
    const [price, setPrice] = useState('10000');
    const [category, setCategory] = useState('MEETING_ROOM');
    const [equipment, setEquipment] = useState<string[]>([]);
    const [allFacilities, setAllFacilities] = useState<any[]>([]); // To map codes to IDs
    const [imageUrl, setImageUrl] = useState('');
    const [fetchedOfficeId, setFetchedOfficeId] = useState<number | null>(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        // Fetch all active facilities for mapping codes to IDs
        roomApi.getActiveFacilities()
            .then(data => setAllFacilities(data))
            .catch(err => console.error("Failed to fetch facilities", err));

        if (isEditMode) {
            setLoading(true);
            roomApi.getRoomById(Number(roomId))
                .then(room => {
                    setName(room.name);
                    setDescription(room.description || '');
                    setFloor(String(room.floor));
                    setRoomCode(room.roomCode);
                    setCapacity(String(room.capacity));
                    setPrice(String(room.price)); 
                    setCategory(room.category);
                    
                    // Map FacilityResponse[] to facilityCode string[]
                    const codes = room.facilities?.map(f => f.facilityCode) || [];
                    setEquipment(codes);
                    
                    setImageUrl(room.imageUrl || '');
                    setFetchedOfficeId(room.officeId);
                })
                .catch(err => {
                    console.error("Failed to fetch room", err);
                    toast.error('회의실 정보를 불러오지 못했습니다.');
                })
                .finally(() => setLoading(false));
        }
    }, [isEditMode, roomId]);

    const handleCheck = (id: string) => {
        setEquipment(prev =>
            prev.includes(id)
                ? prev.filter(item => item !== id)
                : [...prev, id]
        );
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setLoading(true);

        const currentOfficeId = officeId ? Number(officeId) : fetchedOfficeId;
        
        // Map selected equipment codes (string[]) to facility IDs (number[]) for backend
        const facilityIds = equipment.map(code => 
            allFacilities.find(f => f.facilityCode === code)?.id
        ).filter(id => id !== undefined);

        const roomData = {
            officeId: currentOfficeId,
            name,
            description,
            floor: Number(floor),
            roomCode,
            capacity: Number(capacity),
            price: Number(price),
            category,
            facilityIds, // Map string[] to ID[]
            imageUrl
        };

        try {
            if (isEditMode) {
                await roomApi.updateRoom(Number(roomId), roomData);
                toast.success('회의실 정보가 수정되었습니다.');
            } else {
                await roomApi.createRoom(Number(currentOfficeId), roomData);
                toast.success('회의실이 추가되었습니다.');
            }

            // Navigate back logic
            // If we have officeId in params, we can go back to management page directly
            if (officeId) {
                navigate(`/office/${officeId}/manage`);
            } else {
                navigate(-1);
            }
        } catch (error) {
            console.error(error);
            toast.error('저장 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container mx-auto p-md max-w-2xl">
            <h1 className="text-2xl font-bold mb-lg">
                {isEditMode ? '회의실 정보 수정' : '새 회의실 등록'}
            </h1>

            <form onSubmit={handleSubmit} className="card bg-white shadow-sm p-lg">

                {/* 이름 + 카테고리 */}
                <div className="grid grid-cols-2 gap-md">
                    <Input
                        label="회의실 이름"
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        required
                        fullWidth
                        placeholder="예: 포커스룸 A"
                    />
                    <div className="input-wrapper">
                        <label className="input-label">카테고리</label>
                        <select
                            className="input-field w-full"
                            value={category}
                            onChange={(e) => setCategory(e.target.value)}
                        >
                            <option value="MEETING_ROOM">일반 회의실</option>
                            <option value="CONFERENCE_HALL">컨퍼런스 홀</option>
                            <option value="FOCUS_ROOM">집중실</option>
                            <option value="STUDIO">스튜디오</option>
                        </select>
                    </div>
                </div>

                {/* 층 + 호수 */}
                <div className="grid grid-cols-2 gap-md">
                    <Input
                        label="층 (-1, 1, 2...)"
                        type="number"
                        value={floor}
                        onChange={(e) => setFloor(e.target.value)}
                        required
                        fullWidth
                        placeholder="예: 3"
                    />
                    <Input
                        label="호수"
                        type="text"
                        value={roomCode}
                        onChange={(e) => setRoomCode(e.target.value)}
                        required
                        fullWidth
                        placeholder="예: 301"
                    />
                </div>

                {/* 수용 인원 + 정비 시간 */}
                <div className="grid grid-cols-2 gap-md">
                    {/* 수용 인원 스테퍼 */}
                    <div className="input-wrapper">
                        <label className="input-label">수용 인원 (명)</label>
                        <div style={{ display: 'flex', alignItems: 'center', border: '1px solid #e2e8f0', borderRadius: 'var(--radius-md)', overflow: 'hidden', height: '44px' }}>
                            <button type="button"
                                onClick={() => setCapacity(v => String(Math.max(1, Number(v) - 1)))}
                                style={{ width: '44px', height: '100%', border: 'none', background: 'var(--color-bg-hover)', cursor: 'pointer', fontSize: '1.2rem', flexShrink: 0 }}>−</button>
                            <span style={{ flex: 1, textAlign: 'center', fontWeight: 700, fontSize: '1rem' }}>{capacity}명</span>
                            <button type="button"
                                onClick={() => setCapacity(v => String(Number(v) + 1))}
                                style={{ width: '44px', height: '100%', border: 'none', background: 'var(--color-bg-hover)', cursor: 'pointer', fontSize: '1.2rem', flexShrink: 0 }}>+</button>
                        </div>
                    </div>

                    <Input
                        label="정비 시간 (분)"
                        type="number"
                        value={'0'}
                        onChange={() => {}}
                        fullWidth
                        placeholder="0"
                    />
                </div>

                {/* 시간당 가격 */}
                <div className="input-wrapper">
                    <label className="input-label">시간당 가격 (원)</label>
                    <div style={{ display: 'flex', alignItems: 'center', border: '1px solid #e2e8f0', borderRadius: 'var(--radius-md)', overflow: 'hidden', height: '44px' }}>
                        <button type="button"
                            onClick={() => setPrice(v => String(Math.max(0, Number(v) - 1000)))}
                            style={{ width: '44px', height: '100%', border: 'none', background: 'var(--color-bg-hover)', cursor: 'pointer', fontSize: '1.2rem', flexShrink: 0 }}>−</button>
                        <input
                            type="number"
                            value={price}
                            onChange={(e) => setPrice(e.target.value)}
                            min={0}
                            step={1000}
                            required
                            style={{ flex: 1, textAlign: 'center', fontWeight: 700, fontSize: '1rem', border: 'none', outline: 'none', background: 'transparent' }}
                        />
                        <span style={{ paddingRight: '12px', color: 'var(--color-text-sub)', fontSize: '0.875rem' }}>원</span>
                        <button type="button"
                            onClick={() => setPrice(v => String(Number(v) + 1000))}
                            style={{ width: '44px', height: '100%', border: 'none', background: 'var(--color-bg-hover)', cursor: 'pointer', fontSize: '1.2rem', flexShrink: 0 }}>+</button>
                    </div>
                    <p style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: '4px' }}>
                        {Number(price) > 0 ? `${Number(price).toLocaleString()}원 / 시간` : '무료'}
                    </p>
                </div>

                {/* 시설/장비 */}
                <div className="mt-md">
                    <label className="block text-sm font-bold mb-sm">시설 / 장비</label>
                    <div className="grid grid-cols-2 gap-sm">
                        {CHECK_OPTIONS.map((opt) => (
                            <label key={opt.id} style={{
                                display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer',
                                padding: '10px 12px', borderRadius: 'var(--radius-md)',
                                border: `1px solid ${equipment.includes(opt.id) ? 'var(--color-primary)' : '#e2e8f0'}`,
                                background: equipment.includes(opt.id) ? 'var(--color-primary-glow)' : '#fff',
                                transition: 'all 0.15s',
                            }}>
                                <input
                                    type="checkbox"
                                    checked={equipment.includes(opt.id)}
                                    onChange={() => handleCheck(opt.id)}
                                    style={{ accentColor: 'var(--color-primary)', width: '16px', height: '16px' }}
                                />
                                <span className="text-sm" style={{ fontWeight: equipment.includes(opt.id) ? 600 : 400 }}>{opt.label}</span>
                            </label>
                        ))}
                    </div>
                </div>

                {/* 설명 */}
                <div className="mt-md">
                    <label className="block text-sm font-medium mb-xs">회의실 설명 <span style={{ color: 'var(--color-error)' }}>*</span></label>
                    <textarea
                        className="input-field w-full"
                        style={{ minHeight: '100px', resize: 'vertical' }}
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        placeholder="회의실에 대한 설명을 입력하세요. (예: 최대 8인 수용 가능한 대형 회의실)"
                        required
                        minLength={5}
                    />
                </div>

                {/* 이미지 URL */}
                <div className="mt-md">
                    <Input
                        label="대표 이미지 URL (선택)"
                        type="url"
                        value={imageUrl}
                        onChange={(e) => setImageUrl(e.target.value)}
                        fullWidth
                        placeholder="https://..."
                    />
                    {imageUrl && (
                        <div className="mt-sm" style={{ borderRadius: 'var(--radius-md)', overflow: 'hidden', border: '1px solid #e2e8f0', background: '#f8fafc' }}>
                            <img
                                src={imageUrl}
                                alt="미리보기"
                                style={{ width: '100%', height: '160px', objectFit: 'cover', display: 'block' }}
                                onError={(e) => {
                                    (e.target as HTMLImageElement).style.display = 'none';
                                    (e.target as HTMLImageElement).nextElementSibling!.removeAttribute('hidden');
                                }}
                            />
                            <p hidden style={{ padding: '1rem', textAlign: 'center', color: 'var(--color-text-muted)', fontSize: '0.875rem' }}>
                                이미지를 불러올 수 없습니다.
                            </p>
                        </div>
                    )}
                </div>

                <div className="flex gap-md mt-xl">
                    <button
                        type="button"
                        onClick={() => navigate(-1)}
                        className="btn btn-secondary flex-1"
                    >
                        취소
                    </button>
                    <button
                        type="submit"
                        className="btn btn-primary flex-1"
                        disabled={loading}
                    >
                        {loading ? '저장 중...' : (isEditMode ? '수정하기' : '추가하기')}
                    </button>
                </div>
            </form>
        </div>
    );
}
