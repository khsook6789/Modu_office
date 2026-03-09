import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { favoriteApi, type Favorite } from './api/favorite.api';
import { roomApi } from '../rooms/api/room.api';
import './FavoritesPage.css';

// Extend Favorite locally to hold the fetched image
interface FavoriteWithImage extends Favorite {
    displayImageUrl?: string | null;
}

export default function FavoritesPage() {
    const [favorites, setFavorites] = useState<FavoriteWithImage[]>([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        loadFavorites();
    }, []);

    const loadFavorites = async () => {
        setLoading(true);
        try {
            const data = await favoriteApi.getMyFavorites();
            
            // Backend doesn't provide image in favorite response, so we fetch it via room API
            const favoritesWithImages = await Promise.all(
                data.map(async (fav) => {
                    try {
                        const roomData = await roomApi.getRoomById(fav.roomId);
                        return {
                            ...fav,
                            // Priority: Room response bannerImageUrl > Room response imageUrl > Favorite response imageUrl
                            displayImageUrl: roomData.bannerImageUrl || roomData.imageUrl || (roomData.images && roomData.images.length > 0 ? roomData.images[0].imageUrl : undefined) || fav.imageUrl || 'https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&q=80&w=1000'
                        };
                    } catch (e) {
                        return { ...fav, displayImageUrl: fav.imageUrl || 'https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&q=80&w=1000' };
                    }
                })
            );
            
            setFavorites(favoritesWithImages);
        } catch (err) {
            console.error('즐겨찾기 로드 실패', err);
        } finally {
            setLoading(false);
        }
    };

    const handleRemove = async (roomId: number, roomName: string) => {
        if (!window.confirm(`"${roomName}"을 즐겨찾기에서 삭제하시겠습니까?`)) return;
        try {
            await favoriteApi.removeFavorite(roomId);
            setFavorites(prev => prev.filter(f => f.roomId !== roomId));
        } catch {
            alert('삭제에 실패했습니다.');
        }
    };

    return (
        <div className="favorites-page">
            <div className="favorites-header">
                <h1 className="favorites-title">❤️ 즐겨찾기 <span>회의실</span></h1>
                <p className="favorites-subtitle">관심 있는 공간을 모아두고 빠르게 예약하세요.</p>
            </div>

            {loading ? (
                <div className="favorites-empty">
                    <div className="favorites-empty-icon">⏳</div>
                    <p>불러오는 중...</p>
                </div>
            ) : favorites.length === 0 ? (
                <div className="favorites-empty">
                    <div className="favorites-empty-icon">💔</div>
                    <p>아직 즐겨찾기한 공간이 없어요.</p>
                    <Link to="/rooms" className="favorites-cta">회의실 둘러보기 →</Link>
                </div>
            ) : (
                <div className="favorites-grid">
                    {favorites.map(fav => (
                        <div key={fav.id} className="fav-card">
                            <div
                                className="fav-card-image"
                                style={{ backgroundImage: fav.displayImageUrl ? `url(${fav.displayImageUrl})` : 'linear-gradient(135deg,#1e3a5f,#0ea5e9)' }}
                                onClick={() => navigate(`/rooms/${fav.roomId}`)}
                            >
                                <button
                                    className="fav-remove-btn"
                                    onClick={(e) => { e.stopPropagation(); handleRemove(fav.roomId, fav.roomName); }}
                                    title="즐겨찾기 삭제"
                                >
                                    ❤️
                                </button>
                            </div>
                            <div className="fav-card-body" onClick={() => navigate(`/rooms/${fav.roomId}`)}>
                                <p className="fav-office-name">{fav.officeName}</p>
                                <h3 className="fav-room-name">{fav.roomName}</h3>
                                <div className="fav-meta">
                                    <span>🏢 {fav.floor}층</span>
                                    <span>👥 {fav.capacity}명</span>
                                </div>
                                <button
                                    className="fav-book-btn"
                                    onClick={(e) => { e.stopPropagation(); navigate(`/rooms/${fav.roomId}/book`); }}
                                >
                                    예약하기
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
