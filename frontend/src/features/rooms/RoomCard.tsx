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
    floor?: number;
    roomCode?: string;
    description?: string;
    category?: string;
    bufferTime?: number;
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
                    {/* Rating Badge Overlay - 실제 데이터 있을 때만 표시 */}
                    {room.rating != null && (
                        <div className="rating-badge" style={{
                            position: 'absolute', top: '10px', right: '10px',
                            background: 'rgba(0,0,0,0.6)', color: '#fbbf24',
                            padding: '4px 8px', borderRadius: '12px',
                            fontSize: '0.8rem', fontWeight: 'bold', backdropFilter: 'blur(4px)'
                        }}>
                            ⭐ {room.rating.toFixed(1)}
                        </div>
                    )}
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
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                            {room.category && (
                                <span style={{ fontSize: '0.72rem', fontWeight: 600, color: 'var(--color-primary)', background: 'var(--color-primary-glow)', borderRadius: '999px', padding: '0.15rem 0.6rem' }}>
                                    {room.category}
                                </span>
                            )}
                            <span className="room-location">📍 {room.floor}층 - {room.roomCode}</span>
                        </div>
                    </div>
                    {room.description && (
                        <p className="room-description" style={{ fontSize: '0.85rem', color: '#64748b', margin: '0.5rem 0 0.75rem', overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', lineHeight: '1.4' }}>
                            {room.description}
                        </p>
                    )}

                    <div className="room-meta">
                        <span className="room-badge badge-capacity">👥 {room.capacity} People</span>
                        {room.equipment.slice(0, 2).map((item, index) => (
                            <span key={index} className="room-badge">🔧 {item}</span>
                        ))}
                        {room.equipment.length > 2 && (
                            <span className="room-badge">+{room.equipment.length - 2}</span>
                        )}
                    </div>

                    <div className="room-footer">
                        <div className="status-indicator">
                            <span className={`status-dot ${room.isAvailable ? 'status-available' : 'status-occupied'}`}></span>
                            <span style={{ color: room.isAvailable ? '#10b981' : '#64748b' }}>
                                {room.isAvailable ? '예약 가능' : '사용 중'}
                            </span>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            {room.price !== undefined && room.price > 0 && (
                                <span style={{ fontWeight: 'bold', color: 'var(--color-primary)', fontSize: '0.9rem' }}>{room.price.toLocaleString()}원/시간</span>
                            )}
                            <span className="btn-view-details">상세 보기</span>
                        </div>
                    </div>
                </div>
            </Link>
        </div>
    );
}
