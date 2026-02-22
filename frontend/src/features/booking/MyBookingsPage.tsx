import { useState, useEffect } from 'react';
import { client } from '../../api/client';
import { useAuth } from '../../contexts/AuthContext';

interface ReservationResponse {
    id: number;
    title: string;
    officeId: number;
    officeName: string;
    roomId: number;
    roomName: string;
    roomCode: string;
    customerId: number;
    customerName: string;
    startAt: string;
    endAt: string;
    status: 'PENDING' | 'CONFIRMED' | 'CANCELED';
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
    const [bookings, setBookings] = useState<ReservationResponse[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (user?.id) {
            loadBookings();
        }
    }, [user]);

    const loadBookings = async () => {
        try {
            setIsLoading(true);
            setError(null);
            const response = await client.get<ApiResponse<ReservationResponse[]>>(
                `/reservations?customerId=${user!.id}`
            );
            const list: ReservationResponse[] = (response as any).data ?? [];
            // 최신순 정렬
            const sorted = [...list].sort(
                (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
            );
            setBookings(sorted);
        } catch (err: any) {
            console.error('Failed to load bookings:', err);
            setError('예약 목록을 불러오는 데 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleCancel = async (bookingId: number) => {
        if (!window.confirm('정말로 이 예약을 취소하시겠습니까?')) return;
        try {
            await client.post(`/reservations/${bookingId}/cancel`, null);
            alert('예약이 취소되었습니다.');
            loadBookings();
        } catch (err: any) {
            alert('예약 취소 실패: ' + (err?.message || '서버 오류'));
        }
    };

    const today = new Date();

    const statusLabel = (booking: ReservationResponse) => {
        if (booking.status === 'CANCELED') return { text: '취소됨', color: '#6b7280', bg: '#f3f4f6' };
        const end = new Date(booking.endAt);
        if (end < today) return { text: '이용 완료', color: '#6b7280', bg: '#f3f4f6' };
        if (booking.status === 'PENDING') return { text: '승인 대기', color: '#d97706', bg: '#fef3c7' };
        return { text: '예약 확정', color: '#15803d', bg: '#dcfce7' };
    };

    const formatDateTime = (dt: string) => {
        const d = new Date(dt);
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    };

    if (isLoading) {
        return (
            <div className="container mx-auto p-md max-w-4xl" style={{ textAlign: 'center', padding: '60px' }}>
                <p>로딩 중...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="container mx-auto p-md max-w-4xl" style={{ textAlign: 'center', padding: '60px', color: '#ef4444' }}>
                <p>{error}</p>
                <button className="btn btn-primary" style={{ marginTop: '16px' }} onClick={loadBookings}>다시 시도</button>
            </div>
        );
    }

    return (
        <div className="container mx-auto p-md max-w-4xl">
            <h1 className="text-2xl font-bold mb-lg">내 예약 목록</h1>

            {bookings.length === 0 ? (
                <div style={{
                    textAlign: 'center', padding: '60px 20px',
                    background: 'var(--color-bg-card)',
                    borderRadius: 'var(--radius-lg)',
                    color: 'var(--color-text-muted)',
                }}>
                    <p style={{ fontSize: '40px', marginBottom: '12px' }}>📋</p>
                    <p style={{ fontSize: '16px', fontWeight: 600 }}>예약 내역이 없습니다</p>
                    <p style={{ fontSize: '14px', marginTop: '6px' }}>회의실을 예약하면 여기에 표시됩니다.</p>
                </div>
            ) : (
                <div className="grid gap-md">
                    {bookings.map((booking) => {
                        const label = statusLabel(booking);
                        const isCancelled = booking.status === 'CANCELED';
                        const isPast = new Date(booking.endAt) < today;
                        const canCancel = !isCancelled && !isPast;

                        return (
                            <div
                                key={booking.id}
                                className="card p-md"
                                style={{
                                    background: 'var(--color-bg-card)',
                                    opacity: isCancelled ? 0.6 : 1,
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    alignItems: 'flex-start',
                                    gap: '16px',
                                    flexWrap: 'wrap',
                                }}
                            >
                                <div style={{ flex: 1 }}>
                                    {/* 상태 뱃지 */}
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                                        <span style={{
                                            padding: '3px 10px',
                                            borderRadius: '999px',
                                            fontSize: '12px',
                                            fontWeight: 600,
                                            background: label.bg,
                                            color: label.color,
                                        }}>
                                            {label.text}
                                        </span>
                                        <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                                            {booking.createdAt.substring(0, 10)} 예약
                                        </span>
                                    </div>

                                    <h3 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '4px' }}>
                                        {booking.roomName}
                                        <span style={{ fontSize: '13px', fontWeight: 400, color: 'var(--color-text-muted)', marginLeft: '8px' }}>
                                            ({booking.roomCode})
                                        </span>
                                    </h3>
                                    {booking.title && (
                                        <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', marginBottom: '6px' }}>
                                            📝 {booking.title}
                                        </p>
                                    )}

                                    <div style={{ fontSize: '14px', color: 'var(--color-text-muted)', display: 'flex', flexDirection: 'column', gap: '4px', marginTop: '8px' }}>
                                        <span>🏢 {booking.officeName}</span>
                                        <span>🕐 {formatDateTime(booking.startAt)} ~ {formatDateTime(booking.endAt)}</span>
                                    </div>
                                </div>

                                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '12px' }}>
                                    {booking.totalPrice != null ? (
                                        <span style={{ fontSize: '20px', fontWeight: 700, color: 'var(--color-primary)' }}>
                                            {Number(booking.totalPrice).toLocaleString()}원
                                        </span>
                                    ) : (
                                        <span style={{ fontSize: '14px', color: 'var(--color-text-muted)' }}>가격 미정</span>
                                    )}

                                    {canCancel && (
                                        <button
                                            onClick={() => handleCancel(booking.id)}
                                            style={{
                                                padding: '6px 14px',
                                                borderRadius: 'var(--radius-md)',
                                                border: '1px solid #fca5a5',
                                                background: 'transparent',
                                                color: '#ef4444',
                                                fontSize: '13px',
                                                cursor: 'pointer',
                                            }}
                                        >
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
