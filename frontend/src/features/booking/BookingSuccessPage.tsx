import { useNavigate } from 'react-router-dom';

export default function BookingSuccessPage() {
    const navigate = useNavigate();

    return (
        <div className="container mx-auto p-xl text-center flex flex-col items-center justify-center min-h-[60vh]">
            <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mb-lg">
                <svg className="w-10 h-10 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 13l4 4L19 7"></path>
                </svg>
            </div>

            <h1 className="text-3xl font-bold mb-md">예약이 완료되었습니다!</h1>
            <p className="text-muted mb-xl">
                성공적으로 예약되었습니다.<br />
                예약 내역은 '내 예약' 메뉴에서 확인하실 수 있습니다.
            </p>

            <div className="flex gap-md">
                <button
                    onClick={() => navigate('/')}
                    className="btn btn-outline px-lg"
                >
                    홈으로
                </button>
                <button
                    onClick={() => navigate('/my-bookings')}
                    className="btn btn-primary px-lg"
                >
                    내 예약 확인
                </button>
            </div>
        </div>
    );
}
