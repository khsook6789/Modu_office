import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { type RoomResponse } from '../rooms/api/room.api';
import { client } from '../../api/client';
import './BookingPage.css';

// 임시 예약 저장소 (localStorage)
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
    status: 'CONFIRMED';
    createdAt: string;
}

function saveMockBooking(booking: Omit<MockBooking, 'id' | 'createdAt'>): MockBooking {
    const existing: MockBooking[] = JSON.parse(localStorage.getItem(BOOKINGS_KEY) || '[]');
    const newBooking: MockBooking = {
        ...booking,
        id: Date.now(),
        createdAt: new Date().toISOString(),
    };
    existing.push(newBooking);
    localStorage.setItem(BOOKINGS_KEY, JSON.stringify(existing));
    return newBooking;
}

const PAYMENT_METHODS = [
    { id: 'card', label: '💳 신용/체크카드', desc: '카드로 간편하게 결제' },
    { id: 'kakao', label: '💛 카카오페이', desc: '카카오페이로 빠른 결제' },
    { id: 'naver', label: '💚 네이버페이', desc: '네이버페이로 결제' },
    { id: 'bank', label: '🏦 계좌이체', desc: '실시간 계좌이체' },
];

export default function BookingPage() {
    const { roomId } = useParams();
    const navigate = useNavigate();
    const [room, setRoom] = useState<RoomResponse | null>(null);

    // 예약 폼 상태
    const [date, setDate] = useState('');
    const [startTime, setStartTime] = useState('09:00');
    const [duration, setDuration] = useState('1');
    const [guestCount, setGuestCount] = useState('1');
    const [totalPrice, setTotalPrice] = useState(0);

    // 결제 모달 상태
    const [showPaymentModal, setShowPaymentModal] = useState(false);
    const [selectedPayment, setSelectedPayment] = useState('card');
    const [isProcessing, setIsProcessing] = useState(false);

    useEffect(() => {
        if (roomId) {
            client.get(`/rooms/${roomId}`)
                .then((res: any) => {
                    const data = res.data.data ? res.data.data : res.data;
                    setRoom(data);
                    const today = new Date().toISOString().split('T')[0];
                    setDate(today);
                })
                .catch(err => {
                    console.error('회의실 정보 불러오기 실패', err);
                    alert('존재하지 않는 회의실입니다.');
                    navigate('/rooms');
                });
        }
    }, [roomId, navigate]);

    useEffect(() => {
        if (room) {
            const pricePerHour = 10000;
            setTotalPrice(Number(duration) * pricePerHour);
        }
    }, [duration, room]);

    const calculateEndTime = (start: string, durationHours: number) => {
        const [hours, minutes] = start.split(':').map(Number);
        const endHours = hours + durationHours;
        return `${String(endHours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
    };

    const handleNextStep = () => {
        if (!date || !startTime) {
            alert('날짜와 시간을 선택해주세요.');
            return;
        }
        setShowPaymentModal(true);
    };

    const handleConfirmPayment = () => {
        if (!room) return;
        setIsProcessing(true);

        // 결제 처리 시뮬레이션 (1.5초)
        setTimeout(() => {
            const endTime = calculateEndTime(startTime, Number(duration));
            const booking = saveMockBooking({
                roomId: Number(roomId),
                roomName: room.name,
                date,
                startTime,
                endTime,
                guestCount: Number(guestCount),
                totalPrice,
                paymentMethod: selectedPayment,
                status: 'CONFIRMED',
            });

            setIsProcessing(false);
            setShowPaymentModal(false);
            navigate(`/booking/success/${booking.id}`);
        }, 1500);
    };

    if (!room) return <div className="p-xl text-center text-muted">회의실 정보를 불러오는 중...</div>;

    return (
        <div className="booking-page-container">
            {/* Header Content */}
            <div className="booking-header">
                <h1 className="booking-title">예약 진행하기</h1>
                <p className="booking-subtitle">원하시는 일정과 인원을 선택하시면 결제가 진행됩니다.</p>
            </div>

            {/* Room Info Hero Banner */}
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
                        10,000원 <span style={{fontSize: '0.9rem', color: '#94a3b8'}}>/ 시간</span>
                    </div>
                </div>
            </div>

            {/* Split Layout */}
            <div className="booking-content-grid">
                
                {/* Left Column: Form Settings */}
                <div className="booking-form-section shadow-subtle">
                    <h3 className="booking-section-title">일정 및 인원 선택</h3>
                    
                    <div className="booking-input-row">
                        <div className="form-group" style={{ width: '100%' }}>
                            <label className="input-label booking-label">이용 날짜</label>
                            <input
                                type="date"
                                className="input-field booking-custom-input"
                                value={date}
                                onChange={(e) => setDate(e.target.value)}
                                required
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
                                required
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
                                    onClick={() => {
                                        const current = parseInt(guestCount);
                                        if (current > 1) setGuestCount((current - 1).toString());
                                    }}
                                    disabled={parseInt(guestCount) <= 1}
                                >
                                    -
                                </button>
                                <div className="guest-count-display">{guestCount}명</div>
                                <button
                                    className="btn-counter"
                                    onClick={() => {
                                        const current = parseInt(guestCount);
                                        if (current < room.capacity) setGuestCount((current + 1).toString());
                                    }}
                                    disabled={parseInt(guestCount) >= room.capacity}
                                >
                                    +
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Right Column: Summary & Submit */}
                <div className="booking-summary-widget">
                    <h3 className="booking-section-title" style={{ borderBottom: 'none', marginBottom: '1.5rem' }}>결제 요약</h3>
                    
                    <div className="summary-row">
                        <span>선택 날짜</span>
                        <span className="summary-value">{date || '-'}</span>
                    </div>
                    
                    <div className="summary-row">
                        <span>이용 시간</span>
                        <span className="summary-value" style={{ color: 'var(--color-primary)' }}>
                            {startTime} ~ {calculateEndTime(startTime, Number(duration))} ({duration}시간)
                        </span>
                    </div>
                    
                    <div className="summary-row">
                        <span>참여 인원</span>
                        <span className="summary-value">{guestCount}명</span>
                    </div>

                    <div className="summary-divider"/>

                    <div className="summary-total">
                        <span className="total-label">총 결제 금액</span>
                        <span className="total-amount">{totalPrice.toLocaleString()}원</span>
                    </div>

                    <button
                        onClick={handleNextStep}
                        className="btn-book-submit"
                    >
                        결제하기
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

            {/* Payment Modal */}
            {showPaymentModal && (
                <div className="payment-modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) setShowPaymentModal(false); }}>
                    <div className="payment-modal-card">
                        <h2 className="text-2xl font-bold mb-xs" style={{ color: 'var(--color-text-main)' }}>결제 수단 선택</h2>
                        <p className="text-muted mb-lg" style={{ fontSize: '0.95rem' }}>
                            {room.name} · {date} · {startTime}~{calculateEndTime(startTime, Number(duration))}
                        </p>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '2rem' }}>
                            {PAYMENT_METHODS.map((method) => (
                                <div
                                    key={method.id}
                                    className={`payment-method-btn ${selectedPayment === method.id ? 'selected' : ''}`}
                                    onClick={() => setSelectedPayment(method.id)}
                                >
                                    <span className="payment-method-icon">{method.label.split(' ')[0]}</span>
                                    <div className="payment-method-info">
                                        <div className="payment-method-name">{method.label.split(' ').slice(1).join(' ')}</div>
                                        <div className="payment-method-desc">{method.desc}</div>
                                    </div>
                                    {selectedPayment === method.id && (
                                        <div style={{ color: 'var(--color-primary)', fontSize: '1.5rem' }}>✓</div>
                                    )}
                                </div>
                            ))}
                        </div>

                        <div className="summary-total" style={{ margin: 0, padding: '1rem', background: 'var(--color-bg-hover)', borderRadius: '1rem', border: '1px solid #e2e8f0' }}>
                            <span className="total-label" style={{ fontSize: '1rem' }}>최종 결제 금액</span>
                            <span className="total-amount" style={{ fontSize: '1.5rem' }}>{totalPrice.toLocaleString()}원</span>
                        </div>

                        <div className="flex gap-md mt-lg">
                            <button
                                className="btn btn-secondary flex-1"
                                onClick={() => setShowPaymentModal(false)}
                                disabled={isProcessing}
                                style={{ padding: '1rem', background: 'transparent', border: '1px solid #cbd5e1', color: 'var(--color-text-sub)' }}
                            >
                                뒤로
                            </button>
                            <button
                                className="btn-book-submit flex-1"
                                onClick={handleConfirmPayment}
                                disabled={isProcessing}
                                style={{ margin: 0, padding: '1rem', fontSize: '1rem' }}
                            >
                                {isProcessing ? '처리 중...' : '결제 승인'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
