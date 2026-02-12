import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

export default function PendingApprovalPage() {
    const { logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    return (
        <div className="flex flex-col items-center justify-center min-h-[60vh] text-center p-md">
            <div className="bg-white p-lg rounded-lg shadow-md max-w-md w-full">
                <div className="text-4xl mb-md">⏳</div>
                <h1 className="text-2xl font-bold mb-sm text-primary">승인 대기 중</h1>
                <p className="text-muted mb-lg">
                    회원님의 계정은 현재 관리자 승인 대기 중입니다.<br />
                    승인이 완료되면 서비스를 이용하실 수 있습니다.
                </p>
                <div className="flex flex-col gap-sm">
                    <button
                        onClick={handleLogout}
                        className="btn btn-outline w-full"
                    >
                        로그아웃
                    </button>
                    <button
                        onClick={() => window.location.reload()}
                        className="text-sm text-muted hover:underline mt-sm"
                    >
                        다시 확인하기
                    </button>
                </div>
            </div>
        </div>
    );
}
