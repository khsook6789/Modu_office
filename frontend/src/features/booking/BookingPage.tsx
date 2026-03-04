import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { loadPaymentWidget } from '@tosspayments/payment-widget-sdk';
import { type RoomResponse } from '../rooms/api/room.api';
import { client } from '../../api/client';
import { useAuth } from '../../contexts/AuthContext';
import './BookingPage.css';

// 토스 테스트 클라이언트 키 (공개값)
const TOSS_CLIENT_KEY = 'test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm';

interface ApiResponse<T> { status: string; message: string; data: T; }
interface ReservationResponse { id: number; totalPrice: number | null; }

export default function BookingPage() {
    const { roomId } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();
    const [room, setRoom] = useState<RoomResponse | null>(null);

    const [date, setDate] = useState('');
    const [startTime, setStartTime] = useState('09:00');
    const [duration, setDuration] = useState('1');
    const [guestCount, setGuestCount] = useState('1');
    const [totalPrice, setTotalPrice] = useState(0);
    const [title, setTitle] = useState('');

    const [isProcessing, setIsProcessing] = useState(false);

    // 방 정보 불러오기
    useEffect(() => {
        if (!roomId) return;
        client.get(`/rooms/${roomId}`)
            .then((res: any) => {
                const data = res?.data?.data ?? res?.data ?? res;
                setRoom(data);
                setDate(new Date().toISOString().split('T')[0]);
            })
            .catch(() => { alert('존재하지 않는 회의실입니다.'); navigate('/rooms'); });
    }, [roomId, navigate]);

    // 가격 계산 (방의 pricePerHour 필드 없으면 임시 10,000원)
    useEffect(() => {
        if (!room) return;
        const pricePerHour = (room as any).pricePerHour ?? 10000;
        setTotalPrice(Number(duration) * pricePerHour);
    }, [duration, room]);

    const calculateEndTimeForUI = (start: string, addHours: number) => {
        const [h, m] = start.split(':').map(Number);
        return `${String((h + addHours) % 24).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
    };

    const calculateEndDateTimeISO = (dateStr: string, timeStr: string, addHours: number) => {
        const dateObj = new Date(`${dateStr}T${timeStr}:00`);
        dateObj.setHours(dateObj.getHours() + addHours);
        const y = dateObj.getFullYear();
        const m = String(dateObj.getMonth() + 1).padStart(2, '0');
        const d = String(dateObj.getDate()).padStart(2, '0');
        const h = String(dateObj.getHours()).padStart(2, '0');
        const min = String(dateObj.getMinutes()).padStart(2, '0');
        return `${y}-${m}-${d}T${h}:${min}:00`;
    };

    const generateOrderId = (reservationId: number) => {
        // 토스 orderId: 영문자/숫자/- 6~64자
        return `ORDER-${reservationId}-${Date.now()}`.slice(0, 64);
    };

    const handlePayment = async () => {
        if (!date || !startTime || !room) {
            alert('날짜와 시간을 선택해주세요.');
            return;
        }
        setIsProcessing(true);
        try {
            const endAt = calculateEndDateTimeISO(date, startTime, Number(duration));
            const startAt = `${date}T${startTime}:00`;

            // 1. 예약 생성
            const resResponse = await client.post<ApiResponse<ReservationResponse>>('/reservations', {
                roomId: Number(roomId),
                officeId: Number((room as any).officeId),
                userId: Number(user?.id),
                title: title || `${room.name} 예약`,
                startAt,
                endAt,
                guestCount: Number(guestCount),
            });
            const reservation = resResponse.data ?? (resResponse as any).data;
            const reservationId: number = reservation?.id ?? (resResponse as any).id;
            const orderId = generateOrderId(reservationId);

            // 2. 토스 위젯 로드 & 결제 요청
            const widget = await loadPaymentWidget(TOSS_CLIENT_KEY, String(reservationId));

            const successUrl = `${window.location.origin}/booking/success?orderId=${orderId}&reservationId=${reservationId}&amount=${totalPrice}`;
            const failUrl = `${window.location.origin}/rooms/${roomId}/book?error=payment_failed`;

            await widget.requestPayment({
                orderId,
                orderName: `${room.name} (${date} ${startTime}~${endTimeUI})`,
                successUrl,
                failUrl,
                customerName: undefined,
                amount: totalPrice,
            } as any);

        } catch (err: any) {
            console.error('결제 오류', err);
            if (err?.code !== 'USER_CANCEL') {
                alert('결제 중 오류가 발생했습니다: ' + (err?.message || err));
            }
        } finally {
            setIsProcessing(false);
        }
    };

    if (!room) return <div className="p-xl text-center text-muted">회의실 정보를 불러오는 중...</div>;

    const endTimeUI = calculateEndTimeForUI(startTime, Number(duration));

    return (
        <div className="booking-page-container">
            <div className="booking-header">
                <h1 className="booking-title">예약 진행하기</h1>
                <p className="booking-subtitle">원하시는 일정과 인원을 선택하시면 결제가 진행됩니다.</p>
            </div>

            {/* Room Info Banner */}
            <div className="booking-room-banner shadow-subtle">
                <img
                    src={room.imageUrl || 'https://via.placeholder.com/400x300'}
                    alt={room.name}
                    className="booking-room-img"
                />
                <div className="booking-room-details">
                    <h2 className="booking-room-name">{room.name}</h2>
                    <div className="booking-room-info">
                        <span>👥 최대 {room.capacity}명 수용</span>
                        <span>📍 {room.floor}층 - {room.roomCode}</span>
                    </div>
                    <div className="booking-room-price">
                        {((room as any).pricePerHour ?? 10000).toLocaleString()}원{' '}
                        <span style={{ fontSize: '0.9rem', color: '#94a3b8' }}>/ 시간</span>
                    </div>
                </div>
            </div>

            <div className="booking-content-grid">
                {/* Left: Form */}
                <div className="booking-form-section shadow-subtle">
                    <h3 className="booking-section-title">일정 및 인원 선택</h3>

                    <div className="booking-input-row">
                        <div className="form-group" style={{ width: '100%' }}>
                            <label className="input-label booking-label">예약 제목</label>
                            <input
                                type="text"
                                className="input-field booking-custom-input"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                placeholder={`${room.name} 예약`}
                            />
                        </div>
                    </div>

                    <div className="booking-input-row">
                        <div className="form-group" style={{ width: '100%' }}>
                            <label className="input-label booking-label">이용 날짜</label>
                            <input
                                type="date"
                                className="input-field booking-custom-input"
                                value={date}
                                onChange={(e) => setDate(e.target.value)}
                                min={new Date().toISOString().split('T')[0]}
                            />
                        </div>
                        <div className="form-group" style={{ width: '100%' }}>
                            <label className="input-label booking-label">시작 시간</label>
                            <input
                                type="time"
                                className="input-field booking-custom-input"
                                value={startTime}
                                onChange={(e) => setStartTime(e.target.value)}
                                min="09:00"
                                max="22:00"
                            />
                        </div>
                    </div>

                    <div className="booking-input-row">
                        <div className="form-group" style={{ width: '100%' }}>
                            <label className="input-label booking-label">이용 시간</label>
                            <select
                                className="input-field booking-custom-select"
                                value={duration}
                                onChange={(e) => setDuration(e.target.value)}
                            >
                                <option value="1">1시간</option>
                                <option value="2">2시간</option>
                                <option value="3">3시간</option>
                                <option value="4">4시간</option>
                                <option value="8">8시간 (종일)</option>
                            </select>
                        </div>
                        <div className="form-group" style={{ width: '100%' }}>
                            <label className="input-label booking-label">참여 인원</label>
                            <div className="guest-counter">
                                <button
                                    className="btn-counter"
                                    onClick={() => setGuestCount(c => String(Math.max(1, Number(c) - 1)))}
                                    disabled={Number(guestCount) <= 1}
                                >-</button>
                                <div className="guest-count-display">{guestCount}명</div>
                                <button
                                    className="btn-counter"
                                    onClick={() => setGuestCount(c => String(Math.min(room.capacity, Number(c) + 1)))}
                                    disabled={Number(guestCount) >= room.capacity}
                                >+</button>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Right: Summary */}
                <div className="booking-summary-widget">
                    <h3 className="booking-section-title" style={{ borderBottom: 'none', marginBottom: '1.5rem' }}>결제 요약</h3>

                    <div className="summary-row"><span>선택 날짜</span><span className="summary-value">{date || '-'}</span></div>
                    <div className="summary-row">
                        <span>이용 시간</span>
                        <span className="summary-value" style={{ color: 'var(--color-primary)' }}>
                            {startTime} ~ {endTimeUI} ({duration}시간)
                        </span>
                    </div>
                    <div className="summary-row"><span>참여 인원</span><span className="summary-value">{guestCount}명</span></div>

                    <div className="summary-divider" />

                    <div className="summary-total">
                        <span className="total-label">총 결제 금액</span>
                        <span className="total-amount">{totalPrice.toLocaleString()}원</span>
                    </div>

                    <button
                        onClick={handlePayment}
                        className="btn-book-submit"
                        disabled={isProcessing}
                    >
                        {isProcessing ? '처리 중...' : '토스페이로 결제하기'}
                    </button>

                    <button
                        onClick={() => navigate(-1)}
                        className="btn btn-outline w-full mt-sm"
                        style={{ border: 'none', color: '#94a3b8' }}
                    >
                        취소 및 뒤로가기
                    </button>
                </div>
            </div>
        </div>
    );
}
