import { GoogleMap, LoadScript, Marker, InfoWindow } from '@react-google-maps/api';
import { useState } from 'react';
import { type Office } from '../../features/rooms/api/office.api';

const containerStyle = {
    width: '100%',
    height: '400px',
    borderRadius: '12px'
};

const center = {
    lat: 37.5665, // Seoul City Hall default
    lng: 126.9780
};

interface OfficeMapProps {
    offices: Office[];
}

export default function OfficeMap({ offices }: OfficeMapProps) {
    const [selectedOffice, setSelectedOffice] = useState<Office | null>(null);

    const mapCenter = offices.length > 0 
        ? { lat: offices[0].latitude, lng: offices[0].longitude }
        : center;

    return (
        <LoadScript googleMapsApiKey={import.meta.env.VITE_GOOGLE_MAPS_API_KEY || ''}>
            <GoogleMap
                mapContainerStyle={containerStyle}
                center={mapCenter}
                zoom={13}
            >
                {offices.map(office => (
                    <Marker
                        key={office.id}
                        position={{ lat: office.latitude, lng: office.longitude }}
                        onClick={() => setSelectedOffice(office)}
                    />
                ))}

                {selectedOffice && (
                    <InfoWindow
                        position={{ lat: selectedOffice.latitude, lng: selectedOffice.longitude }}
                        onCloseClick={() => setSelectedOffice(null)}
                    >
                        <div style={{ padding: '8px', color: '#000' }}>
                            <h3 style={{ fontWeight: 'bold', marginBottom: '4px' }}>{selectedOffice.name}</h3>
                            <p style={{ fontSize: '14px', marginBottom: '8px' }}>{selectedOffice.location}</p>
                            <p style={{ fontSize: '12px', color: '#666' }}>
                                {selectedOffice.openTime} - {selectedOffice.closeTime}
                            </p>
                        </div>
                    </InfoWindow>
                )}
            </GoogleMap>
        </LoadScript>
    );
}
