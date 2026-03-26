import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { paymentApi } from './api/payment.api';
import { client } from '../../api/client';
import { type PendingReservationInfo } from './BookingPage';

interface ApiResponse<T> { status: string; message: string; data: T; }
interface ReservationResponse { id: number; }

type Status = 'loading' | 'success' | 'error';

/**
 * 결제 및 예약 성공 페이지
 * logic: Toss SuccessUrl을 통해 전달된 파라미터(paymentKey 등)를 백엔드에 전달하여 결제를 확정함.
 */
export default function BookingSuccessPage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [status, setStatus] = useState<Status>('loading');
    const [errorMsg, setErrorMsg] = useState('');
    
    // React StrictMode에서 useEffect가 두 번 실행되는 것을 방지하기 위한 Ref
    const hasRun = useRef(false);

    useEffect(() => {
        const paymentKey = searchParams.get('paymentKey');
        const orderId = searchParams.get('orderId');
        const amount = searchParams.get('amount');
        const reservationIdFromUrl = searchParams.get('reservationId');

        // 결제 파라미터가 없으면 에러 또는 이전 성공 상태 유지
        if (!paymentKey || !orderId || !amount) {
            if (status !== 'success') {
                setStatus('error');
                setErrorMsg('결제 승인에 필요한 정보가 부족합니다.');
            }
            return;
        }

        const run = async () => {
            if (hasRun.current) return;
            hasRun.current = true;

            try {
                let finalReservationId: number | null = null;

                // 1. URL 파라미터 확인
                if (reservationIdFromUrl && reservationIdFromUrl !== 'undefined') {
                    finalReservationId = Number(reservationIdFromUrl);

                } 
                
                // 2. URL에 없으면 sessionStorage의 lastReservationId 확인
                if (!finalReservationId || isNaN(finalReservationId)) {
                    const savedId = sessionStorage.getItem('lastReservationId');
                    if (savedId) {
                        finalReservationId = Number(savedId);

                    }
                }

                // 3. 하위 호환: 여전히 없으면 pendingReservation으로 생성 시도
                if (!finalReservationId || isNaN(finalReservationId)) {
                    const raw = sessionStorage.getItem('pendingReservation');
                    if (raw) {

                        const info: PendingReservationInfo = JSON.parse(raw);
                        const resRes = await client.post<ApiResponse<ReservationResponse>>('/reservations', {
                            roomId: info.roomId,
                            officeId: info.officeId,
                            userId: info.userId,
                            title: info.title,
                            startAt: info.startAt,
                            endAt: info.endAt,
                            guestCount: info.guestCount,
                        });
                        const data = (resRes as any).data ?? resRes;
                        finalReservationId = data.id;

                    }
                }

                if (!finalReservationId) {
                    throw new Error('예약 번호를 확인할 수 없습니다.');
                }



                // 2. 백엔드 결제 승인 (이 과정에서 결제 정보 검증 및 예약 확정 처리)
                await paymentApi.confirm({
                    paymentKey,
                    orderId,
                    amount: Number(amount),
                    reservationId: finalReservationId,
                });

                // 성공 시 임시 데이터 정리
                sessionStorage.removeItem('pendingReservation');
                sessionStorage.removeItem('lastReservationId');
                setStatus('success');

            } catch (err: any) {
                console.error('결제 승인 처리 중 에러:', err);
                // 400 에러 등의 경우 백엔드에서 온 메시지 사용
                const msg = err?.response?.data?.message || err?.message || '결제 처리 중 오류가 발생했습니다.';
                setErrorMsg(msg);
                setStatus('error');
            }
        };

        run();
    }, [searchParams]); // eslint-disable-line react-hooks/exhaustive-deps

    if (status === 'loading') {
        return (
            <div className="container mx-auto p-xl text-center flex flex-col items-center justify-center min-h-[60vh]">
                <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>⏳</div>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700 }}>결제 승인 확인 중...</h1>
                <p style={{ color: '#94a3b8', marginTop: '0.5rem' }}>잠시만 기다려 주세요.</p>
            </div>
        );
    }

    if (status === 'error') {
        return (
            <div className="container mx-auto p-xl text-center flex flex-col items-center justify-center min-h-[60vh]">
                <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>❌</div>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#ef4444' }}>예약/결제 처리 실패</h1>
                <p style={{ color: '#94a3b8', marginTop: '0.5rem', marginBottom: '2rem' }}>{errorMsg}</p>
                <div className="flex gap-md">
                    <button onClick={() => navigate(-1)} className="btn btn-outline px-lg">뒤로 가기</button>
                    <button onClick={() => navigate('/my-bookings')} className="btn btn-primary px-lg">내 예약 확인</button>
                </div>
            </div>
        );
    }

    return (
        <div className="container mx-auto p-xl text-center flex flex-col items-center justify-center min-h-[60vh]">
            <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mb-lg">
                <svg className="w-10 h-10 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 13l4 4L19 7"></path>
                </svg>
            </div>

            <h1 className="text-3xl font-bold mb-md">예약 및 결제가 완료되었습니다!</h1>
            <p className="text-muted mb-xl text-lg">
                결제가 성공적으로 처리되었습니다.<br />
                관리자 승인 후 예약이 최종 확정됩니다.<br />
                예약 내역은 '내 예약' 메뉴에서 확인하실 수 있습니다.
            </p>

            <div className="flex gap-md">
                <button onClick={() => navigate('/')} className="btn btn-outline px-lg">홈으로</button>
                <button onClick={() => navigate('/my-bookings')} className="btn btn-primary px-lg">내 예약 확인</button>
            </div>
        </div>
    );
}
