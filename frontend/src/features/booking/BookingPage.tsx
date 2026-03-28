import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { loadPaymentWidget, type PaymentWidgetInstance } from '@tosspayments/payment-widget-sdk';
import { type RoomResponse } from '../rooms/api/room.api';
import { client } from '../../api/client';
import { useAuth } from '../../contexts/AuthContext';
import { useToast } from '../../components/Toast';
import Calendar from '../../components/Calendar/Calendar';
import './BookingPage.css';

// 토스 테스트 클라이언트 키 (공개값)
const TOSS_CLIENT_KEY = 'test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm';

interface ApiResponse<T> { status: string; message: string; data: T; }

// sessionStorage에 임시 저장할 예약 정보 타입
export interface PendingReservationInfo {
    roomId: number;
    officeId: number;
    userId: number;
    title: string;
    startAt: string;
    endAt: string;
    guestCount: number;
    amount: number;
}

export default function BookingPage() {
    const { roomId } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();
    const toast = useToast();
    const [room, setRoom] = useState<RoomResponse | null>(null);

    const [date, setDate] = useState('');
    const [startTime, setStartTime] = useState('09:00');
    const [duration, setDuration] = useState('1');
    const [guestCount, setGuestCount] = useState('1');
    const [totalPrice, setTotalPrice] = useState(0);
    const [title, setTitle] = useState('');

    const [isProcessing, setIsProcessing] = useState(false);

    // 결제 위젯 관련 상태
    const [paymentWidget, setPaymentWidget] = useState<PaymentWidgetInstance | null>(null);
    const [isWidgetLoading, setIsWidgetLoading] = useState(false);
    const [step, setStep] = useState<'form' | 'payment'>('form');

    // 방 정보 불러오기
    useEffect(() => {
        if (!roomId) return;
        client.get<ApiResponse<RoomResponse>>(`/rooms/${roomId}`)
            .then((res) => {
                const data = (res as any).data ?? res;
                setRoom(data);
                setDate(new Date().toISOString().split('T')[0]);
            })
            .catch(() => { toast.error('존재하지 않는 회의실입니다.'); navigate('/rooms'); });
    }, [roomId, navigate]);

    // 가격 계산
    useEffect(() => {
        if (!room) return;
        const pricePerHour = room.price || 10000;
        setTotalPrice(Number(duration) * pricePerHour);
    }, [duration, room]);

    // 결제 위젯 마운트 (step === 'payment' 로 전환된 후)
    useEffect(() => {
        if (step !== 'payment' || totalPrice <= 0) return;

        let cancelled = false;
        setIsWidgetLoading(true);

        const customerKey = `user-${user?.id ?? 'guest'}`;

        loadPaymentWidget(TOSS_CLIENT_KEY, customerKey)
            .then(async (widget) => {
                if (cancelled) return;
                await widget.renderPaymentMethods(
                    '#payment-method',
                    { value: totalPrice }
                );
                if (!cancelled) {
                    setPaymentWidget(widget);
                    setIsWidgetLoading(false);
                }
            })
            .catch((err) => {
                if (!cancelled) {
                    console.error('위젯 로드 실패', err);
                    toast.error('결제 위젯 로드에 실패했습니다. 다시 시도해 주세요.');
                    setStep('form');
                    setIsWidgetLoading(false);
                }
            });

        return () => { cancelled = true; };
    }, [step, totalPrice, user?.id]);

    const calculateEndTimeForUI = (start: string, addHours: number) => {
        const [h, m] = start.split(':').map(Number);
        return `${String((h + addHours) % 24).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
    };

    const calculateEndDateTimeISO = (dateStr: string, timeStr: string, addHours: number) => {
        const dateObj = new Date(`${dateStr}T${timeStr}:00`);
        dateObj.setHours(dateObj.getHours() + addHours);
        const y = dateObj.getFullYear();
        const mo = String(dateObj.getMonth() + 1).padStart(2, '0');
        const d = String(dateObj.getDate()).padStart(2, '0');
        const h = String(dateObj.getHours()).padStart(2, '0');
        const min = String(dateObj.getMinutes()).padStart(2, '0');
        return `${y}-${mo}-${d}T${h}:${min}:00`;
    };

    // 랜덤 orderId 생성 (reservationId 불필요 — 예약을 아직 생성하지 않음)
    const generateOrderId = () => {
        const rand = Math.random().toString(36).substring(2, 8).toUpperCase();
        return `PAYMENT-${Date.now()}-${rand}`.slice(0, 64);
    };

    // 1단계: 폼 유효성 검사 후 결제 UI 표시 (예약 생성 X)
    const handleProceedToPayment = () => {
        if (!date || !startTime || !room) {
            toast.warning('날짜와 시간을 선택해주세요.');
            return;
        }
        if (Number(guestCount) < 1 || Number(guestCount) > room.capacity) {
            toast.warning(`참여 인원은 1명 이상 ${room.capacity}명 이하여야 합니다.`);
            return;
        }

        const endAt = calculateEndDateTimeISO(date, startTime, Number(duration));
        const startAt = `${date}T${startTime}:00`;

        // 예약 정보를 sessionStorage에 임시 저장 (결제 성공 후 생성)
        const info: PendingReservationInfo = {
            roomId: Number(roomId),
            officeId: Number((room as any).officeId),
            userId: Number(user?.id),
            title: title || `${room.name} 예약`,
            startAt,
            endAt,
            guestCount: Number(guestCount),
            amount: totalPrice,
        };
        sessionStorage.setItem('pendingReservation', JSON.stringify(info));

        setStep('payment');
    };

    // 2단계: 결제 요청 — 예약을 먼저 생성(PENDING)한 뒤 결제 위젯 호출
    const handlePayment = async () => {
        if (!paymentWidget || !room) return;

        const raw = sessionStorage.getItem('pendingReservation');
        if (!raw) {
            toast.error('예약 정보가 유실되었습니다. 다시 시도해주세요.');
            setStep('form');
            return;
        }

        setIsProcessing(true);
        try {
            const info: PendingReservationInfo = JSON.parse(raw);

            // 1. 서버에 예약 먼저 생성 (PENDING 상태)
            const res = await client.post<ApiResponse<any>>('/reservations', {
                roomId: info.roomId,
                officeId: info.officeId,
                userId: info.userId,
                title: info.title,
                startAt: info.startAt,
                endAt: info.endAt,
                guestCount: info.guestCount,
            });


            const reservationId = res.data?.id ?? (res as any).id;

            
            if (!reservationId) throw new Error('예약 생성에 실패했습니다.');

            // 리다이렉트 시 유실 방지를 위해 세션에도 저장
            sessionStorage.setItem('lastReservationId', String(reservationId));

            // 2. 결제 요청
            const orderId = generateOrderId();
            // successUrl에 reservationId를 쿼리 파라미터로 추가하여 SuccessPage에서 중복 생성을 방지함
            const successUrl = `${window.location.origin}/booking/success?reservationId=${reservationId}`;
            const failUrl = `${window.location.origin}/rooms/${roomId}/book?error=payment_failed&reservationId=${reservationId}`;

            await paymentWidget.requestPayment({
                orderId,
                orderName: `${room.name} (${date} ${startTime}~${endTimeUI})`,
                successUrl,
                failUrl,
                customerName: user?.name ?? undefined,
                amount: totalPrice,
            } as any);

        } catch (err: any) {
            console.error('결제/예약 준비 오류', err);
            if (err?.code !== 'USER_CANCEL') {
                toast.error('처리 중 오류가 발생했습니다: ' + (err?.message || '잠시 후 다시 시도해주세요.'));
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
                <p className="booking-subtitle">
                    {step === 'form'
                        ? '원하시는 일정과 인원을 선택하시면 결제가 진행됩니다.'
                        : '결제수단을 선택하고 결제를 완료해 주세요.'}
                </p>
            </div>

            {/* Room Info Banner */}
            <div className="booking-room-banner shadow-subtle">
                <img
                    src={room.bannerImageUrl || (room.images && room.images.length > 0 ? room.images[0].imageUrl : undefined) || room.imageUrl || 'https://via.placeholder.com/400x300'}
                    alt={room.name}
                    className="booking-room-img"
                />
                <div className="booking-room-details">
                    <h2 className="booking-room-name">{room.name}</h2>
                    <div className="booking-room-info">
                        <span>👥 최대 {room.capacity}명 수용</span>
                        <span>📍 {room.floor}층 - {room.roomCode}</span>
                        {room.bufferTime > 0 && <span>🧹 사용 후 {room.bufferTime}분 정비</span>}
                    </div>
                    {room.description && (
                        <p style={{ marginTop: '0.5rem', fontSize: '0.9rem', color: '#64748b', lineHeight: '1.4' }}>
                            {room.description}
                        </p>
                    )}
                    <div className="booking-room-price">
                        {(room.price || 10000).toLocaleString()}원{' '}
                        <span style={{ fontSize: '0.9rem', color: '#94a3b8' }}>/ 시간</span>
                    </div>
                </div>
            </div>

            <div className="booking-content-grid">
                {/* Left: Form 또는 Payment Widget */}
                {step === 'form' ? (
                    <div className="booking-form-section shadow-subtle" key="form">
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

                        <div className="form-group">
                            <label className="input-label booking-label">이용 날짜</label>
                            <Calendar
                                value={date}
                                onChange={setDate}
                                minDate={new Date().toISOString().split('T')[0]}
                            />
                        </div>

                        <div className="booking-input-row">
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
                ) : (
                    <div className="booking-form-section shadow-subtle" style={{ minHeight: 300 }} key="payment">
                        <h3 className="booking-section-title">결제수단 선택</h3>
                        {isWidgetLoading && (
                            <div style={{ textAlign: 'center', padding: '2rem', color: '#94a3b8' }}>
                                결제 위젯 로딩 중...
                            </div>
                        )}
                        {/* 토스 결제수단 위젯이 이 div 안에 렌더링됩니다 */}
                        <div id="payment-method" />
                    </div>
                )}

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

                    {step === 'form' ? (
                        <button
                            onClick={handleProceedToPayment}
                            className="btn-book-submit"
                            disabled={isProcessing}
                        >
                            다음 단계 (결제수단 선택)
                        </button>
                    ) : (
                        <button
                            onClick={handlePayment}
                            className="btn-book-submit"
                            disabled={isProcessing || isWidgetLoading || !paymentWidget}
                        >
                            {isProcessing ? '처리 중...' : '토스페이로 결제하기'}
                        </button>
                    )}

                    <button
                        onClick={() => step === 'payment' ? setStep('form') : navigate(-1)}
                        className="btn btn-outline w-full mt-sm"
                        style={{ border: 'none', color: '#94a3b8' }}
                    >
                        {step === 'payment' ? '← 일정 다시 선택' : '취소 및 뒤로가기'}
                    </button>
                </div>
            </div>
        </div>
    );
}
