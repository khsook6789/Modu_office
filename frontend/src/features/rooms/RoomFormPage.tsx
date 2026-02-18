import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Input from '../../components/Input';
import { roomApi } from './api/room.api';

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
    const isEditMode = !!roomId;

    const [name, setName] = useState('');
    const [floor, setFloor] = useState('1');
    const [roomCode, setRoomCode] = useState('');
    const [capacity, setCapacity] = useState('4');
    const [price, setPrice] = useState('10000');
    const [category, setCategory] = useState('MEETING_ROOM');
    const [equipment, setEquipment] = useState<string[]>([]);
    const [imageUrl, setImageUrl] = useState('');
    const [fetchedOfficeId, setFetchedOfficeId] = useState<number | null>(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (isEditMode) {
            setLoading(true);
            roomApi.getRoomById(Number(roomId))
                .then(room => {
                    setName(room.name);
                    setFloor(String(room.floor));
                    setRoomCode(room.roomCode);
                    setCapacity(String(room.capacity));
                    setPrice('10000'); // Price is not in API response yet, using default
                    setCategory(room.category);
                    setEquipment(room.equipment || []); // Use equipment from room data (need to ensure getRoomById returns it)
                    setImageUrl(room.imageUrl || '');
                    setFetchedOfficeId(room.officeId);
                })
                .catch(err => {
                    console.error("Failed to fetch room", err);
                    alert('회의실 정보를 불러오지 못했습니다.');
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
        
        const roomData = {
            officeId: currentOfficeId,
            name,
            floor: Number(floor),
            roomCode,
            capacity: Number(capacity),
            price: Number(price),
            category,
            equipment,
            imageUrl
        };

        try {
            if (isEditMode) {
                await roomApi.updateRoom(Number(roomId), roomData);
                alert('회의실 정보가 수정되었습니다.');
            } else {
                await roomApi.createRoom(Number(currentOfficeId), roomData);
                alert('회의실이 추가되었습니다.');
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
            alert('저장 중 오류가 발생했습니다.');
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
                <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
                    <Input
                        label="회의실 이름"
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        required
                        fullWidth
                        placeholder="예: Focus Room A"
                    />
                    <Input
                        label="위치 (호수)"
                        type="text"
                        value={roomCode}
                        onChange={(e) => setRoomCode(e.target.value)}
                        required
                        fullWidth
                        placeholder="예: 201"
                    />
                    <Input
                        label="층수"
                        type="number"
                        value={floor}
                        onChange={(e) => setFloor(e.target.value)}
                        required
                        fullWidth
                    />
                    <Input
                        label="수용 인원 (명)"
                        type="number"
                        value={capacity}
                        onChange={(e) => setCapacity(e.target.value)}
                        required
                        fullWidth
                    />
                    <Input
                        label="시간당 가격 (원)"
                        type="number"
                        value={price}
                        onChange={(e) => setPrice(e.target.value)}
                        required
                        fullWidth
                    />

                    <div className="form-group mb-sm">
                        <label className="block text-sm font-medium mb-xs">카테고리</label>
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

                <div className="mt-md">
                    <label className="block text-sm font-bold mb-sm">보유 설비 (Facilities)</label>
                    <div className="grid grid-cols-2 gap-sm">
                        {CHECK_OPTIONS.map((opt) => (
                            <label key={opt.id} className="flex items-center gap-xs cursor-pointer p-xs hover:bg-gray-50 rounded">
                                <input
                                    type="checkbox"
                                    checked={equipment.includes(opt.id)}
                                    onChange={() => handleCheck(opt.id)}
                                    className="w-4 h-4 text-primary"
                                />
                                <span className="text-sm">{opt.label}</span>
                            </label>
                        ))}
                    </div>
                </div>

                <div className="mt-md">
                    <Input
                        label="대표 이미지 URL"
                        type="url"
                        value={imageUrl}
                        onChange={(e) => setImageUrl(e.target.value)}
                        fullWidth
                        placeholder="https://..."
                    />
                    {imageUrl && (
                        <div className="mt-sm p-sm border rounded bg-gray-50 text-center">
                            <img src={imageUrl} alt="Preview" className="h-40 mx-auto object-cover rounded" />
                        </div>
                    )}
                </div>

                <div className="flex gap-md mt-xl">
                    <button
                        type="button"
                        onClick={() => navigate(-1)}
                        className="btn btn-outline flex-1"
                    >
                        취소
                    </button>
                    <button
                        type="submit"
                        className="btn btn-primary flex-1"
                        disabled={loading}
                    >
                        {loading ? '저장 중...' : '저장하기'}
                    </button>
                </div>
            </form>
        </div>
    );
}
