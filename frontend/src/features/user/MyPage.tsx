import { useAuth } from '../../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

export default function MyPage() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleWithdrawal = () => {
        if (window.confirm('정말로 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
            // Call API to delete user
            alert('회원 탈퇴가 완료되었습니다.');
            logout();
            navigate('/login');
        }
    };

    if (!user) return <div>로그인이 필요합니다.</div>;

    return (
        <div className="container mx-auto p-md max-w-4xl">
            <h1 className="text-3xl font-bold mb-xl">마이페이지</h1>

            {/* Content simplified to just profile info since wishlist is gone */}
            <div className="max-w-2xl mx-auto">
                <div className="card bg-white shadow-sm p-xl mb-lg">
                    <div className="flex items-center mb-lg">
                        <div className="w-16 h-16 bg-gray-200 rounded-full flex items-center justify-center text-2xl mr-md font-bold text-gray-600">
                            {user.name.charAt(0).toUpperCase()}
                        </div>
                        <div>
                            <h2 className="text-xl font-bold">{user.name}</h2>
                            <p className="text-muted">{user.email}</p>
                            <span className="inline-block mt-xs px-2 py-0.5 bg-gray-100 text-xs rounded text-gray-600 font-medium">
                                {user.role}
                            </span>
                        </div>
                    </div>

                    <hr className="my-lg border-gray-100" />

                    <h3 className="font-bold mb-md text-lg">계정 설정</h3>
                    <div className="space-y-sm">
                        <button className="w-full text-left p-sm hover:bg-gray-50 rounded flex justify-between items-center transition-colors">
                            <span>비밀번호 변경</span>
                            <span className="text-xs text-muted bg-gray-100 px-2 py-1 rounded">준비중</span>
                        </button>
                        <button className="w-full text-left p-sm hover:bg-gray-50 rounded flex justify-between items-center transition-colors">
                            <span>알림 설정</span>
                            <span className="text-xs text-green-600 bg-green-50 px-2 py-1 rounded font-bold">ON</span>
                        </button>
                    </div>
                </div>

                <div className="text-right mt-lg">
                    <button
                        onClick={handleWithdrawal}
                        className="text-red-500 text-sm hover:underline hover:text-red-700 transition-colors"
                    >
                        회원 탈퇴하기
                    </button>
                </div>
            </div>
        </div>
    );
}
