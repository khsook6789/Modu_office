import { useState, useEffect, useRef, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Autocomplete, GoogleMap, Marker, useJsApiLoader } from '@react-google-maps/api';
import Input from '../../components/Input';
import { officeApi } from '../rooms/api/office.api';

const LIBRARIES: ('places')[] = ['places'];

const mapContainerStyle = {
    width: '100%',
    height: '250px',
    borderRadius: '12px',
    marginTop: '8px',
};

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

    // 좌표 state
    const [latitude, setLatitude] = useState<number>(37.5665);
    const [longitude, setLongitude] = useState<number>(126.9780);
    const [mapCenter, setMapCenter] = useState({ lat: 37.5665, lng: 126.9780 });

    // Autocomplete ref
    const autocompleteRef = useRef<google.maps.places.Autocomplete | null>(null);

    // Google Maps 로드
    const { isLoaded, loadError } = useJsApiLoader({
        googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY || '',
        libraries: LIBRARIES,
    });

    useEffect(() => {
        if (isEditMode) {
            setLoading(true);
            officeApi.getOfficeById(id!)
                .then((data) => {
                    setName(data.name);
                    setDescription((data as any).description || '');
                    setLocation(data.location);
                    setOpenTime(data.openTime);
                    setCloseTime(data.closeTime);
                    if (data.latitude && data.longitude) {
                        setLatitude(data.latitude);
                        setLongitude(data.longitude);
                        setMapCenter({ lat: data.latitude, lng: data.longitude });
                    }
                })
                .catch(err => console.error('Failed to fetch office', err))
                .finally(() => setLoading(false));
        }
    }, [isEditMode, id]);

    // Autocomplete에서 주소 선택됐을 때
    const handlePlaceChanged = () => {
        const place = autocompleteRef.current?.getPlace();
        if (!place || !place.geometry || !place.geometry.location) return;

        const lat = place.geometry.location.lat();
        const lng = place.geometry.location.lng();
        const formattedAddress = place.formatted_address || place.name || '';

        setLocation(formattedAddress);
        setLatitude(lat);
        setLongitude(lng);
        setMapCenter({ lat, lng });
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setLoading(true);

        const officeData = {
            name,
            description,
            location,
            latitude,
            longitude,
            openTime,
            closeTime,
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
                    <label className="block text-sm font-medium mb-xs">오피스 설명 <span style={{ color: 'var(--color-error)' }}>*</span></label>
                    <textarea
                        className="input-field w-full"
                        style={{ minHeight: '100px', resize: 'vertical' }}
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        placeholder="오피스에 대한 설명을 5자 이상 입력해주세요. (예: 강남역 도보 2분 거리의 프리미엄 공유오피스입니다.)"
                        required
                        minLength={5}
                    />
                </div>

                {/* 주소 검색 (Places Autocomplete) */}
                <div className="form-group mb-sm">
                    <label className="block text-sm font-medium mb-xs">
                        위치 (주소) <span style={{ color: 'var(--color-error)' }}>*</span>
                    </label>
                    {isLoaded ? (
                        <Autocomplete
                            onLoad={(ac) => (autocompleteRef.current = ac)}
                            onPlaceChanged={handlePlaceChanged}
                            options={{ componentRestrictions: { country: 'kr' } }}
                        >
                            <input
                                type="text"
                                className="input-field w-full"
                                value={location}
                                onChange={(e) => setLocation(e.target.value)}
                                placeholder="주소를 검색하세요... (예: 서울 강남구 테헤란로)"
                                required
                            />
                        </Autocomplete>
                    ) : (
                        <input
                            type="text"
                            className="input-field w-full"
                            value={location}
                            onChange={(e) => setLocation(e.target.value)}
                            placeholder={loadError ? '지도를 불러오지 못했습니다' : '지도 로딩 중...'}
                            disabled={!loadError}
                            required
                        />
                    )}
                </div>

                {/* 미니 지도 미리보기 */}
                {isLoaded && (
                    <div className="form-group mb-sm">
                        <label className="block text-sm font-medium mb-xs" style={{ color: 'var(--color-text-secondary)' }}>
                            📍 선택된 위치 미리보기
                        </label>
                        <GoogleMap
                            mapContainerStyle={mapContainerStyle}
                            center={mapCenter}
                            zoom={16}
                            options={{
                                disableDefaultUI: true,
                                zoomControl: true,
                                scrollwheel: false,
                            }}
                        >
                            <Marker position={mapCenter} />
                        </GoogleMap>
                        <p style={{ fontSize: '12px', color: 'var(--color-text-secondary)', marginTop: '4px' }}>
                            위도: {latitude.toFixed(6)}, 경도: {longitude.toFixed(6)}
                        </p>
                    </div>
                )}

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
