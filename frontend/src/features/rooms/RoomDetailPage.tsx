import { roomApi } from './api/room.api';
import { client } from '../../api/client';
import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { reviewApi, type Review } from '../reviews/api/review.api';
import ReviewList from '../reviews/components/ReviewList';
import ReviewForm from '../reviews/components/ReviewForm';
import { useAuth } from '../../contexts/AuthContext';
import './RoomDetailPage.css';

export default function RoomDetailPage() {
    // Use params if available, otherwise mock ID
    const { id } = useParams();
    const roomId = id ? Number(id) : 101; // Default to 101 if no ID

    const navigate = useNavigate();
    const { user } = useAuth();
    
    // Room State
    const [room, setRoom] = useState<any>(null); // Ideally use Room type
    const [loadingRoom, setLoadingRoom] = useState(true);

    const [reviews, setReviews] = useState<Review[]>([]);
    const [loadingReviews, setLoadingReviews] = useState(false);
    const [completedReservationId, setCompletedReservationId] = useState<number | null>(null);

    const loadReviews = async () => {
        setLoadingReviews(true);
        try {
            const data = await reviewApi.getReviewsByRoomId(roomId);
            setReviews(data);
        } catch (error) {
            console.error("Failed to load reviews", error);
        } finally {
            setLoadingReviews(false);
        }
    };

    useEffect(() => {
        const fetchRoom = async () => {
            try {
                const data = await roomApi.getRoomById(roomId);
                // Map to UI shape
                setRoom({
                    id: data.id,
                    name: data.name,
                    location: `${data.floor}F - ${data.roomCode}`,
                    capacity: data.capacity,
                    description: '넓은 공간과 최신 장비를 갖춘 프리미엄 회의실입니다.', 
                    equipment: data.equipment || [],
                    imageUrl: data.imageUrl,
                    pricePerHour: 0,
                    rating: (data as any).rating || 0
                });
            } catch (error) {
                console.error("Failed to load room", error);
            } finally {
                setLoadingRoom(false);
            }
        };

        fetchRoom();
        loadReviews();
        // 해당 방의 완료된 예약 조회 (리뷰 작성용)
        if (user?.id) {
            client.get<any>(`/reservations?customerId=${user.id}&roomId=${roomId}`)
                .then((res: any) => {
                    const list = res.data?.content ?? res.data ?? [];
                    const completed = list.find((r: any) => r.roomId === roomId && r.status === 'CONFIRMED');
                    if (completed) setCompletedReservationId(completed.id);
                })
                .catch(() => {});
        }
    }, [roomId, user]);

    if (loadingRoom) {
        return <div className="text-center py-20">Loading room details...</div>;
    }

    if (!room) {
        return <div className="text-center py-20">Room not found</div>;
    }

    const handleReviewSubmit = async (rating: number, comment: string) => {
        if (!user) {
            alert('로그인이 필요합니다.');
            navigate('/login');
            return;
        }

        try {
            await reviewApi.createReview({
                reservationId: completedReservationId!,
                rating,
                content: comment,
            });
            await loadReviews();
        } catch (error) {
            console.error("Failed to submit review", error);
            alert('리뷰 등록에 실패했습니다.');
        }
    };

    const handleDeleteReview = async (reviewId: number) => {
        if (window.confirm('리뷰를 삭제하시겠습니까?')) {
            try {
                await reviewApi.deleteReview(reviewId);
                await loadReviews();
            } catch (error) {
                console.error("Failed to delete review", error);
            }
        }
    };

    const handleBook = () => {
        // Navigate to booking page with room ID
        navigate(`/rooms/${roomId}/book`);
    };

    return (
        <div className="room-detail-page">
            <Link to="/rooms" className="back-link">← 다른 공간 찾아보기</Link>

            <div className="room-title-section">
                <h1>{room.name}</h1>
                <p>📍 {room.location}</p>
            </div>

            <div className="room-hero-image-wrapper">
                <img src={room.imageUrl || 'https://via.placeholder.com/800'} alt={room.name} className="room-hero-image" />
                <div className="room-hero-overlay">
                    <div className="hero-meta">
                        <div className="hero-rating">
                            ⭐ {room.rating ? room.rating.toFixed(1) : 'New'}
                        </div>
                        <p style={{ marginTop: '0.5rem', opacity: 0.9 }}>수용인원: {room.capacity}명</p>
                    </div>
                </div>
            </div>

            <div className="room-detail-grid">
                {/* Left Column: Info & Reviews */}
                <div className="room-main-content">
                    <div className="info-section">
                        <h2 className="section-title">✨ 공간 소개</h2>
                        <p className="info-text">{room.description}</p>
                    </div>

                    <div className="info-section">
                        <h2 className="section-title">🔧 시설 및 제공 장비</h2>
                        <div className="equipment-list">
                            {room.equipment.map((item: string, idx: number) => (
                                <span key={idx} className="equipment-tag">
                                    <span style={{ color: 'var(--color-primary)' }}>✓</span> {item}
                                </span>
                            ))}
                        </div>
                    </div>

                    {/* Review Section */}
                    <div className="info-section">
                        <h2 className="section-title">💬 이용 후기 ({reviews.length})</h2>

                        {user ? (
                            completedReservationId ? (
                                <ReviewForm onSubmit={handleReviewSubmit} />
                            ) : (
                                <div style={{ background: '#f8fafc', padding: '1.5rem', borderRadius: '1rem', textAlign: 'center', marginBottom: '2rem' }}>
                                    <p style={{ color: '#64748b' }}>이 공간을 이용 완료(확정)하신 분만 리뷰를 작성할 수 있습니다.</p>
                                </div>
                            )
                        ) : (
                            <div style={{ background: '#f8fafc', padding: '1.5rem', borderRadius: '1rem', textAlign: 'center', marginBottom: '2rem' }}>
                                <p style={{ color: '#64748b', marginBottom: '0.5rem' }}>후기를 작성하려면 로그인이 필요합니다.</p>
                                <Link to="/login" style={{ color: 'var(--color-primary)', fontWeight: 'bold' }}>로그인하기</Link>
                            </div>
                        )}

                        {loadingReviews ? (
                            <div style={{ textAlign: 'center', padding: '2rem 0', color: '#94a3b8' }}>리뷰를 불러오는 중...</div>
                        ) : (
                            <ReviewList reviews={reviews} onDelete={handleDeleteReview} />
                        )}
                    </div>
                </div>

                {/* Right Column: Sticky Booking Widget */}
                <aside className="booking-sidebar">
                    <div className="booking-widget">
                        <h2 className="widget-title">이 공간 예약하기</h2>
                        <div className="widget-price">
                            {room.pricePerHour ? `${room.pricePerHour.toLocaleString()}원` : '무료'} <span>/ 시간</span>
                        </div>
                        
                        <hr className="widget-divider" />
                        
                        <p style={{ color: '#475569', fontSize: '0.95rem', lineHeight: '1.6', marginBottom: '1.5rem' }}>
                            달력이 있는 페이지로 이동하여<br/>원하는 날짜와 시간을 선택하세요.
                        </p>

                        <button
                            className="btn-book-large"
                            onClick={handleBook}
                        >
                            예약 진행하기
                        </button>
                    </div>

                    {/* Simple Map Widget Placeholder */}
                    <div className="info-section" style={{ marginTop: '2rem', padding: '1.5rem' }}>
                        <h3 className="section-title" style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>위치 정보</h3>
                        <div style={{ width: '100%', height: '180px', background: '#f1f5f9', borderRadius: '1rem', overflow: 'hidden' }}>
                             <iframe
                                title="Google Map"
                                src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3165.379047209633!2d127.02553757640697!3d37.49883582806316!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x357ca13768f54c31%3A0xe54972dd0cc459f!2z6rCV64Ko!5e0!3m2!1sko!2skr!4v1707920000000!5m2!1sko!2skr"
                                width="100%"
                                height="100%"
                                style={{ border: 0 }}
                                allowFullScreen
                                loading="lazy"
                                referrerPolicy="no-referrer-when-downgrade"
                            />
                        </div>
                    </div>
                </aside>
            </div>
        </div>
    );
}

// 빈공간에 구글 지도 넣기