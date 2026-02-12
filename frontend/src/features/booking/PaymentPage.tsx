import { useLocation, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { bookingApi } from './api/booking.api';

export default function PaymentPage() {
    const location = useLocation();
    const navigate = useNavigate();
    const bookingData = location.state;
    const [processing, setProcessing] = useState(false);

    // Redirect if no data
    if (!bookingData) {
        return (
            <div className="p-xl text-center">
                <p>잘못된 접근입니다.</p>
                <button onClick={() => navigate('/')} className="btn btn-primary mt-md">홈으로 가기</button>
            </div>
        );
    }

    const handlePayment = async () => {
        setProcessing(true);
        // Simulate API delay
        setTimeout(async () => {
            try {
                // In real app, we verify payment then confirm booking
                // Here we just pretend to create and confirm
                const booking = await bookingApi.confirmBooking(12345); // Mock ID
                navigate(`/booking/success/${booking.id}`, { state: { booking: { ...bookingData, id: booking.id } } });
            } catch (error) {
                alert('결제 처리에 실패했습니다.');
                setProcessing(false);
            }
        }, 1500);
    };

    return (
        <div className="container mx-auto p-md max-w-lg">
            <h1 className="text-2xl font-bold mb-lg">결제하기</h1>

            <div className="card bg-white shadow-sm p-lg mb-md">
                <h2 className="text-lg font-bold mb-md border-b pb-sm">예약 정보 확인</h2>
                <div className="space-y-sm text-sm">
                    <div className="flex justify-between">
                        <span className="text-muted">회의실</span>
                        <span className="font-medium">{bookingData.roomName}</span>
                    </div>
                    <div className="flex justify-between">
                        <span className="text-muted">날짜</span>
                        <span className="font-medium">{bookingData.date}</span>
                    </div>
                    <div className="flex justify-between">
                        <span className="text-muted">시간</span>
                        <span className="font-medium">{bookingData.startTime} ~ {bookingData.endTime}</span>
                    </div>
                    <div className="flex justify-between">
                        <span className="text-muted">인원</span>
                        <span className="font-medium">{bookingData.guestCount}명</span>
                    </div>
                </div>

                <div className="mt-lg pt-md border-t flex justify-between items-center">
                    <span className="text-lg font-bold">결제 금액</span>
                    <span className="text-2xl font-bold text-primary">{bookingData.totalPrice.toLocaleString()}원</span>
                </div>
            </div>

            <div className="card bg-white shadow-sm p-lg mb-xl">
                <h2 className="text-lg font-bold mb-md">결제 수단</h2>
                <div className="flex gap-sm">
                    <label className="flex-1 border rounded p-md cursor-pointer hover:bg-gray-50 text-center">
                        <input type="radio" name="payment" defaultChecked className="mb-2" />
                        <div className="font-bold">신용카드</div>
                    </label>
                    <label className="flex-1 border rounded p-md cursor-pointer hover:bg-gray-50 text-center">
                        <input type="radio" name="payment" className="mb-2" />
                        <div className="font-bold">계좌이체</div>
                    </label>
                </div>
            </div>

            <button
                onClick={handlePayment}
                className="btn btn-primary w-full py-4 text-lg font-bold rounded-lg shadow-md hover:bg-opacity-90 transition-colors"
                disabled={processing}
            >
                {processing ? '결제 처리 중...' : `${bookingData.totalPrice.toLocaleString()}원 결제하기`}
            </button>
        </div>
    );
}
