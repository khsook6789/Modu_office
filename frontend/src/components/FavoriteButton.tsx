import { useState, useEffect } from 'react';
import { favoriteApi } from '../features/favorites/api/favorite.api';
import './FavoriteButton.css';

interface Props {
    roomId: number;
}

/**
 * 즐겨찾기 토글 버튼 (하트)
 * 회의실 목록/상세 페이지에서 공유해서 사용
 */
export default function FavoriteButton({ roomId }: Props) {
    const [isFav, setIsFav] = useState(false);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        favoriteApi.checkFavorite(roomId)
            .then(v => setIsFav(v))
            .catch(() => {})
            .finally(() => setLoading(false));
    }, [roomId]);

    const toggle = async (e: React.MouseEvent) => {
        e.stopPropagation();
        e.preventDefault();
        if (loading) return;
        setLoading(true);
        try {
            if (isFav) {
                await favoriteApi.removeFavorite(roomId);
                setIsFav(false);
            } else {
                await favoriteApi.addFavorite(roomId);
                setIsFav(true);
            }
        } catch {
            alert('즐겨찾기 처리에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <button
            className={`fav-heart-btn ${isFav ? 'active' : ''}`}
            onClick={toggle}
            title={isFav ? '즐겨찾기 해제' : '즐겨찾기 추가'}
            disabled={loading}
        >
            {isFav ? '❤️' : '🤍'}
        </button>
    );
}
