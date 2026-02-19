import { useState, useEffect } from 'react';

const BOOKINGS_KEY = 'mock_bookings';

interface MockBooking {
    id: number;
    roomId: number;
    roomName: string;
    date: string;
    startTime: string;
    endTime: string;
    guestCount: number;
    totalPrice: number;
    paymentMethod: string;
    status: 'CONFIRMED' | 'CANCELLED';
    createdAt: string;
}

const PAYMENT_LABEL: Record<string, string> = {
    card: '💳 신용/체크카드',
    kakao: '💛 카카오페이',
    naver: '💚 네이버페이',
    bank: '🏦 계좌이체',
};

export default function MyBookingsPage() {
    const [bookings, setBookings] = useState<MockBooking[]>([]);

    useEffect(() => {
        loadBookings();
    }, []);

    const loadBookings = () => {
        const stored: MockBooking[] = JSON.parse(localStorage.getItem(BOOKINGS_KEY) || '[]');
        // 최신순 정렬
        stored.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        setBookings(stored);
    };

    const handleCancel = (bookingId: number) => {
        if (window.confirm('정말로 이 예약을 취소하시겠습니까?')) {
            const stored: MockBooking[] = JSON.parse(localStorage.getItem(BOOKINGS_KEY) || '[]');
            const updated = stored.map(b =>
                b.id === bookingId ? { ...b, status: 'CANCELLED' as const } : b
            );
            localStorage.setItem(BOOKINGS_KEY, JSON.stringify(updated));
            alert('예약이 취소되었습니다.');
            loadBookings();
        }
    };

    const today = new Date();

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
                        const bookingDate = new Date(`${booking.date}T${booking.startTime}`);
                        const isPast = bookingDate < today;
                        const isCancelled = booking.status === 'CANCELLED';

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
                                            background: isCancelled ? '#e5e7eb' :
                                                isPast ? '#f3f4f6' : '#dcfce7',
                                            color: isCancelled ? '#6b7280' :
                                                isPast ? '#6b7280' : '#15803d',
                                        }}>
                                            {isCancelled ? '취소됨' : isPast ? '이용 완료' : '예약 확정'}
                                        </span>
                                        <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                                            {booking.createdAt.substring(0, 10)} 예약
                                        </span>
                                    </div>

                                    <h3 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '4px' }}>
                                        {booking.roomName}
                                    </h3>

                                    <div style={{ fontSize: '14px', color: 'var(--color-text-muted)', display: 'flex', flexDirection: 'column', gap: '4px', marginTop: '8px' }}>
                                        <span>📅 {booking.date}</span>
                                        <span>⏰ {booking.startTime} ~ {booking.endTime}</span>
                                        <span>👥 {booking.guestCount}명</span>
                                        <span>{PAYMENT_LABEL[booking.paymentMethod] || booking.paymentMethod}</span>
                                    </div>
                                </div>

                                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '12px' }}>
                                    <span style={{ fontSize: '20px', fontWeight: 700, color: 'var(--color-primary)' }}>
                                        {booking.totalPrice.toLocaleString()}원
                                    </span>

                                    {!isPast && !isCancelled && (
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
