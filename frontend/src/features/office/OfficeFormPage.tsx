import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Input from '../../components/Input';
import { officeApi } from '../rooms/api/office.api';

export default function OfficeFormPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const isEditMode = !!id;

    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [location, setLocation] = useState('');
    const [openTime, setOpenTime] = useState('09:00');
    const [closeTime, setCloseTime] = useState('18:00');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (isEditMode) {
            // Fetch existing office data
            setLoading(true);
            officeApi.getOfficeById(id!)
                .then((data) => {
                    setName(data.name);
                    setDescription((data as any).description || '');
                    setLocation(data.location);
                    setOpenTime(data.openTime);
                    setCloseTime(data.closeTime);
                })
                .catch(err => console.error("Failed to fetch office", err))
                .finally(() => setLoading(false));
        }
    }, [isEditMode, id]);

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setLoading(true);

        const officeData = {
            name,
            description,
            location,
            latitude: 37.5,
            longitude: 127.0,
            openTime,
            closeTime
        };

        try {
            if (isEditMode) {
                await officeApi.updateOffice(id!, officeData);
                alert('오피스 정보가 수정되었습니다.');
            } else {
                await officeApi.createOffice(officeData);
                alert('새로운 오피스가 등록되었습니다.');
            }
            navigate('/operator');
        } catch (error) {
            console.error(error);
            alert('오피스 저장 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container mx-auto p-md max-w-lg">
            <h1 className="text-2xl font-bold mb-lg">
                {isEditMode ? '오피스 정보 수정' : '새 오피스 등록'}
            </h1>

            <form onSubmit={handleSubmit} className="card bg-white shadow-sm p-lg">
                <Input
                    label="오피스 이름"
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                    fullWidth
                    placeholder="예: 강남 공유오피스 1호점"
                />

                <div className="form-group mb-sm">
                    <label className="block text-sm font-medium mb-xs">오피스 설명 <span style={{color:'var(--color-error)'}}>*</span></label>
                    <textarea
                        className="input-field w-full"
                        style={{ minHeight: '100px', resize: 'vertical' }}
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        placeholder="오피스에 대한 설명을 20자 이상 입력해주세요. (예: 강남역 도보 2분 거리의 프리미엄 공유오피스입니다.)"
                        required
                        minLength={20}
                    />
                </div>

                <Input
                    label="위치 (주소)"
                    type="text"
                    value={location}
                    onChange={(e) => setLocation(e.target.value)}
                    required
                    fullWidth
                    placeholder="서울시 강남구 테헤란로..."
                />

                <div className="grid grid-cols-2 gap-md">
                    <Input
                        label="오픈 시간"
                        type="time"
                        value={openTime}
                        onChange={(e) => setOpenTime(e.target.value)}
                        required
                        fullWidth
                    />
                    <Input
                        label="마감 시간"
                        type="time"
                        value={closeTime}
                        onChange={(e) => setCloseTime(e.target.value)}
                        required
                        fullWidth
                    />
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
