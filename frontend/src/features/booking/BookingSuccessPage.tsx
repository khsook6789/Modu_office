import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { paymentApi } from './api/payment.api';

type Status = 'loading' | 'success' | 'error';

export default function BookingSuccessPage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [status, setStatus] = useState<Status>('loading');
    const [errorMsg, setErrorMsg] = useState('');

    useEffect(() => {
        const paymentKey = searchParams.get('paymentKey');
        const orderId = searchParams.get('orderId');
        const amount = searchParams.get('amount');

        // 토스 successUrl 파라미터가 없으면 단순 성공 화면 (직접 접근)
        if (!paymentKey || !orderId || !amount) {
            setStatus('success');
            return;
        }

        // 결제 confirm API 호출
        paymentApi.confirm({
            paymentKey,
            orderId,
            amount: Number(amount),
        })
            .then(() => setStatus('success'))
            .catch((err: any) => {
                console.error('결제 승인 실패', err);
                setErrorMsg(err?.message || '알 수 없는 오류');
                setStatus('error');
            });
    }, []);

    if (status === 'loading') {
        return (
            <div className="container mx-auto p-xl text-center flex flex-col items-center justify-center min-h-[60vh]">
                <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>⏳</div>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700 }}>결제를 확인하는 중...</h1>
                <p style={{ color: '#94a3b8', marginTop: '0.5rem' }}>잠시만 기다려 주세요.</p>
            </div>
        );
    }

    if (status === 'error') {
        return (
            <div className="container mx-auto p-xl text-center flex flex-col items-center justify-center min-h-[60vh]">
                <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>❌</div>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#ef4444' }}>결제 승인 실패</h1>
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
            <p className="text-muted mb-xl">
                결제가 성공적으로 처리되었습니다.<br />
                매니저 확정 후 예약이 최종 확정됩니다.<br />
                예약 내역은 '내 예약' 메뉴에서 확인하실 수 있습니다.
            </p>

            <div className="flex gap-md">
                <button onClick={() => navigate('/')} className="btn btn-outline px-lg">홈으로</button>
                <button onClick={() => navigate('/my-bookings')} className="btn btn-primary px-lg">내 예약 확인</button>
            </div>
        </div>
    );
}
