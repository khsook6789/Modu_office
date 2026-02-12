import { useState, useEffect, type MouseEvent } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { wishlistApi } from '../wishlist/api/wishlist.api';
import './RoomCard.css';

export interface Room {
    id: string;
    name: string;
    location: string;
    capacity: number;
    equipment: string[];
    imageUrl?: string;
    isAvailable: boolean;
}

interface RoomCardProps {
    room: Room;
}

export default function RoomCard({ room }: RoomCardProps) {
    const { user } = useAuth();
    const [isLiked, setIsLiked] = useState(false);

    useEffect(() => {
        if (user) {
            wishlistApi.isLiked(user.id, Number(room.id)).then(setIsLiked);
        }
    }, [user, room.id]);

    const handleToggleWishlist = async (e: MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();

        if (!user) {
            alert('로그인이 필요합니다.');
            return;
        }

        const newState = await wishlistApi.toggleWishlist(user.id, Number(room.id));
        setIsLiked(newState);
    };

    return (
        <div className="card room-card" style={{ position: 'relative' }}>
            <button
                onClick={handleToggleWishlist}
                className="wishlist-btn"
                style={{
                    position: 'absolute',
                    top: '10px',
                    right: '10px',
                    zIndex: 10,
                    background: 'rgba(0,0,0,0.5)',
                    border: 'none',
                    borderRadius: '50%',
                    width: '32px',
                    height: '32px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    cursor: 'pointer',
                    color: isLiked ? '#ef4444' : 'white',
                    fontSize: '18px',
                    transition: 'all 0.2s'
                }}
            >
                {isLiked ? '♥' : '♡'}
            </button>

            <Link to={`/rooms/${room.id}`} style={{ textDecoration: 'none', display: 'block', height: '100%' }}>
                <div className="room-image-wrapper">
                    {room.imageUrl ? (
                        <img src={room.imageUrl} alt={room.name} className="room-image" />
                    ) : (
                        <div className="room-image flex-center text-muted" style={{ background: '#334155' }}>
                            No Image
                        </div>
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
