import { useCallback, useMemo, useState } from 'react';
import { GoogleMap, Marker, InfoWindow, useJsApiLoader } from '@react-google-maps/api';
import { useNavigate } from 'react-router-dom';
import { type Office } from '../../features/rooms/api/office.api';

const LIBRARIES: ('places')[] = ['places'];

const containerStyle = {
    width: '100%',
    height: '400px',
    borderRadius: '12px',
};

const DEFAULT_CENTER = { lat: 37.5665, lng: 126.9780 }; // 서울시청 기본값

interface OfficeMapProps {
    offices: Office[];
}

export default function OfficeMap({ offices }: OfficeMapProps) {
    const navigate = useNavigate();
    const [selectedOffice, setSelectedOffice] = useState<Office | null>(null);

    const { isLoaded, loadError } = useJsApiLoader({
        googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY || '',
        libraries: LIBRARIES,
    });

    // 모든 오피스의 평균 좌표를 지도 중점으로 계산
    const mapCenter = useMemo(() => {
        if (offices.length === 0) return DEFAULT_CENTER;
        const avgLat = offices.reduce((sum, o) => sum + o.latitude, 0) / offices.length;
        const avgLng = offices.reduce((sum, o) => sum + o.longitude, 0) / offices.length;
        return { lat: avgLat, lng: avgLng };
    }, [offices]);

    const handleMarkerClick = useCallback((office: Office) => {
        setSelectedOffice(office);
    }, []);

    const handleInfoWindowClose = useCallback(() => {
        setSelectedOffice(null);
    }, []);

    if (loadError) {
        return (
            <div style={{
                width: '100%',
                height: '400px',
                borderRadius: '12px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: '#f5f5f5',
                color: '#888',
                fontSize: '14px',
            }}>
                지도를 불러오지 못했습니다. API 키를 확인해 주세요.
            </div>
        );
    }

    if (!isLoaded) {
        return (
            <div style={{
                width: '100%',
                height: '400px',
                borderRadius: '12px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: '#f5f5f5',
                color: '#aaa',
                fontSize: '14px',
            }}>
                지도 로딩 중...
            </div>
        );
    }

    return (
        <GoogleMap
            mapContainerStyle={containerStyle}
            center={mapCenter}
            zoom={offices.length === 1 ? 15 : 12}
            options={{
                disableDefaultUI: false,
                zoomControl: true,
                mapTypeControl: false,
                streetViewControl: false,
                fullscreenControl: true,
            }}
        >
            {offices.map(office => (
                <Marker
                    key={office.id}
                    position={{ lat: office.latitude, lng: office.longitude }}
                    onClick={() => handleMarkerClick(office)}
                    title={office.name}
                />
            ))}

            {selectedOffice && (
                <InfoWindow
                    position={{ lat: selectedOffice.latitude, lng: selectedOffice.longitude }}
                    onCloseClick={handleInfoWindowClose}
                    options={{ pixelOffset: new window.google.maps.Size(0, -30) }}
                >
                    <div style={{ padding: '4px 8px 8px', color: '#111', maxWidth: '220px' }}>
                        <h3 style={{ fontWeight: 700, fontSize: '15px', marginBottom: '4px' }}>
                            {selectedOffice.name}
                        </h3>
                        <p style={{ fontSize: '13px', color: '#555', marginBottom: '6px', lineHeight: 1.4 }}>
                            📍 {selectedOffice.location}
                        </p>
                        <p style={{ fontSize: '12px', color: '#888', marginBottom: '10px' }}>
                            🕐 {selectedOffice.openTime} ~ {selectedOffice.closeTime}
                        </p>
                        <button
                            onClick={() => navigate(`/rooms?officeId=${selectedOffice.id}`)}
                            style={{
                                display: 'inline-block',
                                padding: '6px 14px',
                                background: 'var(--color-primary, #4F46E5)',
                                color: '#fff',
                                borderRadius: '6px',
                                fontSize: '13px',
                                fontWeight: 600,
                                border: 'none',
                                cursor: 'pointer',
                                width: '100%',
                                textAlign: 'center',
                            }}
                        >
                            이 곳의 방 보기 →
                        </button>
                    </div>
                </InfoWindow>
            )}
        </GoogleMap>
    );
}
