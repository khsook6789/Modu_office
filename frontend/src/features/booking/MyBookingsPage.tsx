import { useState, useEffect } from 'react';
import { bookingApi, type Booking } from './api/booking.api';

export default function MyBookingsPage() {
    const [bookings, setBookings] = useState<Booking[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadBookings();
    }, []);

    const loadBookings = async () => {
        try {
            setLoading(true);
            const data = await bookingApi.getMyBookings();
            setBookings(data);
        } catch (error) {
            console.error("Failed to load bookings", error);
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = async (bookingId: number) => {
        if (window.confirm('정말로 이 예약을 취소하시겠습니까?')) {
            try {
                await bookingApi.cancelBooking(bookingId);
                alert('예약이 취소되었습니다.');
                loadBookings(); // Reload to update status
            } catch (error) {
                console.error("Failed to cancel booking", error);
                alert('취소 처리에 실패했습니다.');
            }
        }
    };

    const today = new Date();
    // Sort bookings: Future first, then Past
    const sortedBookings = [...bookings].sort((a, b) => {
        return new Date(b.date + 'T' + b.startTime).getTime() - new Date(a.date + 'T' + a.startTime).getTime();
    });

    return (
        <div className="container mx-auto p-md max-w-4xl">
            <h1 className="text-2xl font-bold mb-lg">내 예약 목록</h1>

            {loading ? (
                <div className="text-center p-xl">로딩 중...</div>
            ) : sortedBookings.length === 0 ? (
                <div className="text-center p-xl bg-gray-50 rounded text-muted">예약 내역이 없습니다.</div>
            ) : (
                <div className="grid gap-md">
                    {sortedBookings.map((booking) => {
                        const bookingDate = new Date(booking.date + 'T' + booking.startTime);
                        const isPast = bookingDate < today;
                        const isCancelled = booking.status === 'CANCELLED';

                        return (
                            <div key={booking.id} className={`card bg-white shadow-sm p-md flex flex-col md:flex-row justify-between gap-md ${isCancelled ? 'opacity-50' : ''}`}>
                                <div>
                                    <div className="flex items-center gap-sm mb-xs">
                                        <span className={`badge ${isCancelled ? 'bg-gray-200 text-gray-500' :
                                            isPast ? 'bg-gray-100 text-gray-500' :
                                                'bg-green-100 text-green-700'
                                            }`}>
                                            {isCancelled ? '취소됨' : isPast ? '이용 완료' : '예약 확정'}
                                        </span>
                                        <span className="text-sm text-muted">{booking.createdAt.substring(0, 10)} 예약</span>
                                    </div>
                                    <h3 className="text-lg font-bold">{booking.roomName}</h3>
                                    <p className="text-gray-600 text-sm">{booking.officeName}</p>
                                    <div className="mt-sm text-sm">
                                        <p>📅 {booking.date}</p>
                                        <p>⏰ {booking.startTime} ~ {booking.endTime} ({booking.guestCount}명)</p>
                                    </div>
                                </div>
                                <div className="flex flex-col justify-between items-end">
                                    <span className="font-bold text-lg">{booking.totalPrice.toLocaleString()}원</span>

                                    {!isPast && !isCancelled && (
                                        <button
                                            onClick={() => handleCancel(booking.id)}
                                            className="btn btn-sm btn-outline text-red-500 border-red-200 hover:bg-red-50 mt-sm"
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
