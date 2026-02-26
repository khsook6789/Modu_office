import { Link } from 'react-router-dom';
import './RoomCard.css';

export interface Room {
    id: string;
    officeId: number;  // Added to support office-room relationship
    name: string;
    location: string;
    capacity: number;
    equipment: string[];
    imageUrl?: string;
    isAvailable: boolean;
    rating?: number; // Added rating
    price?: number; // Added for backend OfficeRoomRequest
}

interface RoomCardProps {
    room: Room;
    isManager?: boolean;
    onDelete?: (id: string) => void;
}

export default function RoomCard({ room, isManager, onDelete }: RoomCardProps) {

    const handleDelete = (e: React.MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();
        if (confirm(`"${room.name}" 회의실을 삭제하시겠습니까?`)) {
            onDelete?.(room.id);
        }
    };

    return (
        <div className="card room-card" style={{ position: 'relative' }}>
            <Link to={`/rooms/${room.id}`} style={{ textDecoration: 'none', display: 'block', height: '100%' }}>
                <div className="room-image-wrapper">
                    {room.imageUrl ? (
                        <img src={room.imageUrl} alt={room.name} className="room-image" />
                    ) : (
                        <div className="room-image flex-center text-muted" style={{ background: '#334155' }}>
                            No Image
                        </div>
                    )}
                    {/* Rating Badge Overlay */}
                    <div className="rating-badge" style={{
                        position: 'absolute', top: '10px', right: '10px',
                        background: 'rgba(0,0,0,0.6)', color: '#fbbf24', // Amber-400
                        padding: '4px 8px', borderRadius: '12px',
                        fontSize: '0.8rem', fontWeight: 'bold', backdropFilter: 'blur(4px)'
                    }}>
                        ⭐ {room.rating ? room.rating.toFixed(1) : 'New'}
                    </div>
                    {/* Delete Button for Operators */}
                    {isManager && (
                        <button
                            onClick={handleDelete}
                            style={{
                                position: 'absolute', top: '10px', left: '10px',
                                background: 'rgba(239,68,68,0.85)', color: 'white',
                                border: 'none', borderRadius: '8px',
                                padding: '4px 10px', fontSize: '0.75rem',
                                cursor: 'pointer', backdropFilter: 'blur(4px)',
                                fontWeight: 'bold'
                            }}
                        >
                            🗑 삭제
                        </button>
                    )}
                </div>

                <div className="room-content">
                    <div className="room-header">
                        <h3 className="room-name">{room.name}</h3>
                        <span className="room-location">📍 {room.location}</span>
                    </div>

                    <div className="room-meta">
                        <span className="badge badge-capacity">👥 {room.capacity} People</span>
                        {room.equipment.slice(0, 2).map((item, index) => (
                            <span key={index} className="badge">🔧 {item}</span>
                        ))}
                        {room.equipment.length > 2 && (
                            <span className="badge">+{room.equipment.length - 2}</span>
                        )}
                    </div>

                    <div className="room-footer">
                        <div className="status-indicator">
                            <span className={`status-dot ${room.isAvailable ? 'status-available' : 'status-occupied'}`}></span>
                            <span className={room.isAvailable ? 'text-success' : 'text-muted'}>
                                {room.isAvailable ? 'Available Now' : 'Occupied'}
                            </span>
                        </div>
                        <span className="btn btn-secondary text-xs">View Details</span>
                    </div>
                </div>
            </Link>
        </div>
    );
}
