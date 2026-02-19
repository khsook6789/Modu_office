import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { type OfficeRoomResponse, roomApi } from '../rooms/api/room.api';
import Input from '../../components/Input';

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
    const [room, setRoom] = useState<OfficeRoomResponse | null>(null);

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
            roomApi.getRoomById(Number(roomId))
                .then(data => {
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

    if (!room) return <div className="p-xl text-center">회의실 정보를 불러오는 중...</div>;

    return (
        <div className="container mx-auto p-md max-w-lg">
            <h1 className="text-2xl font-bold mb-lg">예약하기</h1>

            <div className="card bg-white shadow-sm p-lg mb-md">
                <div className="flex gap-md mb-md border-b pb-md">
                    <img
                        src={room.imageUrl}
                        alt={room.name}
                        className="w-24 h-24 object-cover rounded-md"
                    />
                    <div>
                        <h2 className="text-xl font-bold">{room.name}</h2>
                        <p className="text-muted text-sm">최대 {room.capacity}명 수용</p>
                        <p className="text-primary font-bold mt-xs">10,000원 / 시간</p>
                    </div>
                </div>

                <div className="space-y-md">
                    <Input
                        label="날짜 선택"
                        type="date"
                        value={date}
                        onChange={(e) => setDate(e.target.value)}
                        required
                        fullWidth
                        min={new Date().toISOString().split('T')[0]}
                    />

                    <div className="grid grid-cols-2 gap-md">
                        <Input
                            label="시작 시간"
                            type="time"
                            value={startTime}
                            onChange={(e) => setStartTime(e.target.value)}
                            required
                            fullWidth
                            min="09:00"
                            max="18:00"
                        />
                        <div className="form-group">
                            <label className="block text-sm font-medium mb-xs">이용 시간</label>
                            <select
                                className="input-field w-full"
                                value={duration}
                                onChange={(e) => setDuration(e.target.value)}
                            >
                                <option value="1">1시간</option>
                                <option value="2">2시간</option>
                                <option value="3">3시간</option>
                                <option value="4">4시간</option>
                            </select>
                        </div>
                    </div>

                    {/* 인원 선택 */}
                    <div className="form-group input-wrapper">
                        <label className="input-label text-sm font-bold mb-xs">
                            인원 (최대 {room.capacity}명)
                        </label>
                        <div className="flex items-center gap-xs">
                            <button
                                className="btn btn-secondary h-12 w-12 flex items-center justify-center p-0 text-xl font-bold"
                                onClick={() => {
                                    const current = parseInt(guestCount);
                                    if (current > 1) setGuestCount((current - 1).toString());
                                }}
                                disabled={parseInt(guestCount) <= 1}
                            >
                                -
                            </button>
                            <div className="flex-1 input-container">
                                <input
                                    type="number"
                                    className="input-field text-center font-bold text-lg"
                                    value={guestCount}
                                    readOnly
                                />
                            </div>
                            <button
                                className="btn btn-secondary h-12 w-12 flex items-center justify-center p-0 text-xl font-bold"
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

            {/* 예약 요약 */}
            <div className="card p-md mb-md" style={{ background: 'var(--color-bg-card)' }}>
                <div className="flex justify-between items-center mb-xs">
                    <span className="text-muted text-sm">이용 시간</span>
                    <span className="text-sm font-medium">
                        {startTime} ~ {calculateEndTime(startTime, Number(duration))} ({duration}시간)
                    </span>
                </div>
                <div className="flex justify-between items-center mb-xs">
                    <span className="text-muted text-sm">인원</span>
                    <span className="text-sm font-medium">{guestCount}명</span>
                </div>
                <div className="flex justify-between items-center border-t pt-sm mt-sm">
                    <span className="text-lg font-bold">총 결제 금액</span>
                    <span className="text-2xl font-bold text-primary">{totalPrice.toLocaleString()}원</span>
                </div>
            </div>

            <div className="flex gap-md">
                <button
                    onClick={() => navigate(-1)}
                    className="btn btn-outline flex-1"
                >
                    취소
                </button>
                <button
                    onClick={handleNextStep}
                    className="btn btn-primary flex-1 py-3 text-lg"
                >
                    결제하기
                </button>
            </div>

            {/* 결제 모달 */}
            {showPaymentModal && (
                <div
                    style={{
                        position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                        backgroundColor: 'rgba(0,0,0,0.6)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        zIndex: 1000, backdropFilter: 'blur(8px)',
                        padding: '20px',
                    }}
                    onClick={(e) => { if (e.target === e.currentTarget) setShowPaymentModal(false); }}
                >
                    <div className="card" style={{
                        width: '100%', maxWidth: '420px',
                        background: 'var(--color-bg-card)',
                        borderRadius: 'var(--radius-xl)',
                        padding: '28px',
                    }}>
                        <h2 className="text-xl font-bold mb-xs">결제 수단 선택</h2>
                        <p className="text-muted text-sm mb-lg">
                            {room.name} · {date} · {startTime}~{calculateEndTime(startTime, Number(duration))}
                        </p>

                        {/* 결제 수단 목록 */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '20px' }}>
                            {PAYMENT_METHODS.map((method) => (
                                <button
                                    key={method.id}
                                    onClick={() => setSelectedPayment(method.id)}
                                    style={{
                                        display: 'flex', alignItems: 'center', gap: '14px',
                                        padding: '14px 16px',
                                        borderRadius: 'var(--radius-md)',
                                        border: selectedPayment === method.id
                                            ? '2px solid var(--color-primary)'
                                            : '1px solid var(--color-border)',
                                        background: selectedPayment === method.id
                                            ? 'rgba(var(--color-primary-rgb, 217,190,158), 0.1)'
                                            : 'transparent',
                                        cursor: 'pointer',
                                        textAlign: 'left',
                                        transition: 'all 0.2s',
                                    }}
                                >
                                    <span style={{ fontSize: '22px' }}>{method.label.split(' ')[0]}</span>
                                    <div>
                                        <div style={{ fontWeight: 600, fontSize: '14px', color: 'var(--color-text-main)' }}>
                                            {method.label.split(' ').slice(1).join(' ')}
                                        </div>
                                        <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                                            {method.desc}
                                        </div>
                                    </div>
                                    {selectedPayment === method.id && (
                                        <span style={{ marginLeft: 'auto', color: 'var(--color-primary)', fontWeight: 'bold' }}>✓</span>
                                    )}
                                </button>
                            ))}
                        </div>

                        {/* 최종 금액 */}
                        <div style={{
                            padding: '14px 16px',
                            background: 'var(--color-bg-dark)',
                            borderRadius: 'var(--radius-md)',
                            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                            marginBottom: '20px',
                        }}>
                            <span style={{ fontWeight: 600 }}>최종 결제 금액</span>
                            <span style={{ fontSize: '22px', fontWeight: 700, color: 'var(--color-primary)' }}>
                                {totalPrice.toLocaleString()}원
                            </span>
                        </div>

                        {/* 버튼 */}
                        <div className="flex gap-sm">
                            <button
                                className="btn btn-outline flex-1"
                                onClick={() => setShowPaymentModal(false)}
                                disabled={isProcessing}
                            >
                                취소
                            </button>
                            <button
                                className="btn btn-primary flex-1"
                                onClick={handleConfirmPayment}
                                disabled={isProcessing}
                                style={{ padding: '12px' }}
                            >
                                {isProcessing ? '결제 처리 중...' : `${totalPrice.toLocaleString()}원 결제`}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
