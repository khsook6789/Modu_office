import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { client } from '../../api/client';
import { useAuth } from '../../contexts/AuthContext';
import './MyBookings.css';

interface ReservationResponse {
    id: number;
    title: string;
    officeId: number;
    officeName: string;
    roomId: number;
    roomName: string;
    roomCode: string;
    userId: number;
    userName: string;
    startAt: string;
    endAt: string;
    status: 'PENDING_PAYMENT' | 'PENDING_APPROVAL' | 'CONFIRMED' | 'CANCELED';
    totalPrice: number | null;
    createdAt: string;
}

interface ApiResponse<T> {
    status: string;
    message: string;
    data: T;
}

export default function MyBookingsPage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [bookings, setBookings] = useState<ReservationResponse[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [filter, setFilter] = useState<'ALL' | 'PENDING_PAYMENT' | 'PENDING_APPROVAL' | 'CONFIRMED' | 'CANCELED'>('ALL');

    // 환불 예상액 모달
    const [refundModal, setRefundModal] = useState<{
        bookingId: number;
        totalPrice: number;
        refundAmount: number;
        cancellationPenalty: number;
        refundRate: number;
        reasonDescriptor: string;
    } | null>(null);
    const [cancellingId, setCancellingId] = useState<number | null>(null);

    useEffect(() => {
        if (user?.id) loadBookings();
    }, [user]);

    const loadBookings = async () => {
        try {
            setIsLoading(true);
            setError(null);
            const response = await client.get<ApiResponse<ReservationResponse[]>>(
                `/reservations?userId=${user!.id}`
            );
            // 응답이 배열이거나 data 안에 있는 경우 모두 처리
            const raw = response as any;
            const list: ReservationResponse[] = Array.isArray(raw) ? raw
                : Array.isArray(raw?.data) ? raw.data
                    : Array.isArray(raw?.data?.content) ? raw.data.content
                        : [];
            setBookings([...list].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()));
        } catch (err: any) {
            console.error('Failed to load bookings:', err);
            setError('예약 목록을 불러오는 데 실패했습니다. (데이터가 없거나 서버 오류)');
            setBookings([]);  // 에러여도 빈 목록으로 표시
        } finally {
            setIsLoading(false);
        }
    };

    const handleCancel = async (bookingId: number) => {
        // 1. 환불 예상액 먼저 조회
        try {
            const res = await client.get<any>(`/reservations/${bookingId}/refund-preview`);
            const raw = res?.data ?? res;
            const preview = raw?.data ?? raw;
            setRefundModal({
                bookingId,
                totalPrice: Number(preview.totalPrice ?? 0),
                refundAmount: Number(preview.refundAmount ?? 0),
                cancellationPenalty: Number(preview.cancellationPenalty ?? 0),
                refundRate: preview.refundRate ?? 100,
                reasonDescriptor: preview.reasonDescriptor ?? '',
            });
        } catch {
            // 환불 정보 조회 실패 시 그냥 확인 묻기
            if (window.confirm('정말로 이 예약을 취소하시겠습니까?')) {
                await doCancel(bookingId);
            }
        } finally {
        }
    };

    const doCancel = async (bookingId: number) => {
        setCancellingId(bookingId);
        try {
            await client.post(`/reservations/${bookingId}/cancel`, null);
            setRefundModal(null);
            await loadBookings();
        } catch (err: any) {
            alert('예약 취소 실패: ' + (err?.message || '서버 오류'));
        } finally {
            setCancellingId(null);
        }
    };

    const today = new Date();

    const getStatusInfo = (booking: ReservationResponse) => {
        if (booking.status === 'CANCELED') return { label: '취소됨', cls: 'CANCELED' };
        if (new Date(booking.endAt) < today) return { label: '이용 완료', cls: 'DONE' };
        if (booking.status === 'PENDING_PAYMENT') return { label: '결제 대기', cls: 'PENDING_PAYMENT' };
        if (booking.status === 'PENDING_APPROVAL') return { label: '승인 대기', cls: 'PENDING_APPROVAL' };
        return { label: '예약 확정', cls: 'CONFIRMED' };
    };

    const filtered = filter === 'ALL'
        ? bookings
        : bookings.filter(b => b.status === filter);

    const stats = {
        total: bookings.length,
        pendingPayment: bookings.filter(b => b.status === 'PENDING_PAYMENT').length,
        pendingApproval: bookings.filter(b => b.status === 'PENDING_APPROVAL').length,
        confirmed: bookings.filter(b => b.status === 'CONFIRMED').length,
        canceled: bookings.filter(b => b.status === 'CANCELED').length,
    };

    if (isLoading) return <div className="my-bookings-page"><div className="booking-empty"><p>로딩 중...</p></div></div>;

    return (
        <div className="my-bookings-page">
            {/* 환불 예상액 모달 */}
            {refundModal && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <div style={{ background: '#1e293b', borderRadius: '1.25rem', padding: '2rem', width: '360px', border: '1px solid rgba(255,255,255,0.1)' }}>
                        <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1.25rem', color: '#f1f5f9' }}>🔍 취소 전 환불 예상액</h3>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem', fontSize: '0.9rem', marginBottom: '1rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ color: '#94a3b8' }}>결제 금액</span>
                                <span style={{ color: '#f1f5f9' }}>{refundModal.totalPrice.toLocaleString()}원</span>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ color: '#94a3b8' }}>위약금</span>
                                <span style={{ color: '#ef4444' }}>-{refundModal.cancellationPenalty.toLocaleString()}원</span>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 700, borderTop: '1px solid rgba(255,255,255,0.1)', paddingTop: '0.6rem' }}>
                                <span style={{ color: '#94a3b8' }}>환불 예상액</span>
                                <span style={{ color: '#34d399', fontSize: '1.05rem' }}>{refundModal.refundAmount.toLocaleString()}원</span>
                            </div>
                        </div>
                        {refundModal.reasonDescriptor && (
                            <p style={{ fontSize: '0.78rem', color: '#64748b', background: 'rgba(255,255,255,0.04)', borderRadius: '0.5rem', padding: '0.5rem 0.75rem', marginBottom: '1.25rem' }}>
                                ℹ️ {refundModal.reasonDescriptor}
                            </p>
                        )}
                        <div style={{ display: 'flex', gap: '0.75rem' }}>
                            <button
                                onClick={() => setRefundModal(null)}
                                style={{ flex: 1, padding: '0.75rem', background: 'transparent', border: '1px solid #475569', borderRadius: '0.75rem', color: '#94a3b8', cursor: 'pointer' }}
                            >닫기</button>
                            <button
                                onClick={() => doCancel(refundModal.bookingId)}
                                disabled={cancellingId !== null}
                                style={{ flex: 1, padding: '0.75rem', background: '#ef4444', border: 'none', borderRadius: '0.75rem', color: '#fff', fontWeight: 600, cursor: 'pointer' }}
                            >{cancellingId ? '취소 중...' : '예약 취소 확정'}</button>
                        </div>
                    </div>
                </div>
            )}
            {/* Header */}
            <div className="bookings-header">
                <h1 className="bookings-title">내 <span>예약</span> 내역</h1>
                <button className="bookings-new-btn" onClick={() => navigate('/rooms')}>+ 새 예약하기</button>
            </div>

            {/* Error Banner */}
            {error && (
                <div style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '0.875rem', padding: '0.875rem 1.25rem', marginBottom: '1.25rem', color: '#dc2626', fontSize: '0.875rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>⚠️ {error}</span>
                    <button onClick={loadBookings} style={{ background: '#dc2626', color: '#fff', border: 'none', borderRadius: '0.5rem', padding: '0.3rem 0.75rem', fontSize: '0.8rem', cursor: 'pointer' }}>다시 시도</button>
                </div>
            )}

            {/* Stats */}
            <div className="bookings-stats">
                <div className="book-stat-card">
                    <span className="book-stat-icon">📋</span>
                    <div><div className="book-stat-value">{stats.total}</div><div className="book-stat-label">전체</div></div>
                </div>
                <div className="book-stat-card">
                    <span className="book-stat-icon" style={{ background: '#fffbeb' }}>💳</span>
                    <div><div className="book-stat-value" style={{ color: '#b45309' }}>{stats.pendingPayment}</div><div className="book-stat-label">결제 대기</div></div>
                </div>
                <div className="book-stat-card">
                    <span className="book-stat-icon" style={{ background: '#fef08a' }}>⏳</span>
                    <div><div className="book-stat-value" style={{ color: '#854d0e' }}>{stats.pendingApproval}</div><div className="book-stat-label">승인 대기</div></div>
                </div>
                <div className="book-stat-card">
                    <span className="book-stat-icon" style={{ background: '#f0fdf4' }}>✅</span>
                    <div><div className="book-stat-value" style={{ color: '#15803d' }}>{stats.confirmed}</div><div className="book-stat-label">확정</div></div>
                </div>
                <div className="book-stat-card">
                    <span className="book-stat-icon" style={{ background: '#fef2f2' }}>❌</span>
                    <div><div className="book-stat-value" style={{ color: '#dc2626' }}>{stats.canceled}</div><div className="book-stat-label">취소</div></div>
                </div>
            </div>

            {/* Filter tabs */}
            <div className="book-filter-tabs">
                {[['ALL', '전체'], ['PENDING_PAYMENT', '결제 대기'], ['PENDING_APPROVAL', '승인 대기'], ['CONFIRMED', '확정'], ['CANCELED', '취소']].map(([v, l]) => (
                    <button
                        key={v}
                        className={`book-filter-btn ${filter === v ? 'active' : ''}`}
                        onClick={() => setFilter(v as any)}
                    >{l}</button>
                ))}
            </div>

            {filtered.length === 0 ? (
                <div className="booking-empty">
                    <p style={{ fontSize: '2.5rem', marginBottom: '0.75rem' }}>📭</p>
                    <p style={{ fontWeight: 600, fontSize: '1rem', color: '#334155' }}>예약 내역이 없습니다</p>
                    <p style={{ fontSize: '0.875rem', marginTop: '0.25rem' }}>회의실을 예약하면 여기에 표시됩니다.</p>
                </div>
            ) : (
                <div className="bookings-list">
                    {filtered.map(booking => {
                        const { label, cls } = getStatusInfo(booking);
                        const isPast = new Date(booking.endAt) < today;
                        const canCancel = booking.status !== 'CANCELED' && !isPast;

                        return (
                            <div key={booking.id} className={`booking-card ${cls === 'CANCELED' || cls === 'DONE' ? 'muted' : ''}`}>
                                <div className="booking-card-left">
                                    <div className="booking-card-top">
                                        <span className={`booking-badge ${cls}`}>{label}</span>
                                        <span className="booking-date-created">{booking.createdAt.substring(0, 10)} 예약</span>
                                    </div>
                                    <h3 className="booking-room-name">
                                        {booking.roomName}
                                        <span className="booking-room-code">({booking.roomCode})</span>
                                    </h3>
                                    {booking.title && <p className="booking-title-text">📝 {booking.title}</p>}
                                    <div className="booking-meta">
                                        <span>🏢 {booking.officeName}</span>
                                        <span>🕐 {new Date(booking.startAt).toLocaleString('ko-KR', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })} ~ {new Date(booking.endAt).toLocaleString('ko-KR', { hour: '2-digit', minute: '2-digit' })}</span>
                                    </div>
                                </div>
                                <div className="booking-card-right">
                                    <div className="booking-price">
                                        {booking.totalPrice != null
                                            ? <span>{Number(booking.totalPrice).toLocaleString()}원</span>
                                            : <span className="price-unknown">가격 미정</span>
                                        }
                                    </div>
                                    {canCancel && (
                                        <button className="booking-cancel-btn" onClick={() => handleCancel(booking.id)}>
                                            예약 취소
                                        </button>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
