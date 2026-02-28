import { useAuth } from '../../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import './MyPage.css';

export default function MyPage() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleWithdrawal = () => {
        if (window.confirm('정말로 탈퇴하시겠습니까? 관련 데이터가 모두 삭제됩니다.')) {
            // TODO: Call API to delete user
            alert('회원 탈퇴가 완료되었습니다.');
            logout();
            navigate('/login');
        }
    };

    if (!user) return <div className="mypage-container"><p>로딩 중이거나 로그인이 필요합니다.</p></div>;

    const initial = user.name ? user.name.charAt(0).toUpperCase() : '?';

    return (
        <div className="mypage-container">
            <div className="mypage-content">
                <div className="mypage-header">
                    <h1 className="mypage-title">마이<span>페이지</span></h1>
                </div>

                <div className="mypage-card">
                    {/* Profile Section */}
                    <div className="profile-header">
                        <div className="profile-avatar">
                            {initial}
                        </div>
                        <div className="profile-info">
                            <h2 className="profile-name">{user.name}</h2>
                            <p className="profile-email">{user.email}</p>
                            <span className={`profile-role-badge ${user.role}`}>
                                {user.role === 'USER' ? '일반 사용자' : user.role === 'MANAGER' ? '운영자' : '관리자'}
                            </span>
                        </div>
                    </div>

                    <hr className="mypage-divider" />

                    {/* Settings Section */}
                    <h3 className="settings-section-title">계정 및 알림 설정</h3>
                    <div className="settings-list">
                        <button className="settings-btn" onClick={() => alert('비밀번호 변경 기능은 준비 중입니다.')}>
                            <span>🔒 비밀번호 변경</span>
                            <span className="settings-badge gray">준비중</span>
                        </button>
                        <button className="settings-btn">
                            <span>🔔 알림 수신 동의</span>
                            <span className="settings-badge green">ON</span>
                        </button>
                        {user.role === 'USER' && (
                            <button className="settings-btn" onClick={() => navigate('/my-bookings')}>
                                <span>📋 내 예약 내역 보기</span>
                                <span style={{ color: 'var(--color-primary)' }}>→</span>
                            </button>
                        )}
                        <button className="settings-btn" onClick={logout} style={{ color: '#ef4444' }}>
                            <span>🚪 로그아웃</span>
                        </button>
                    </div>

                    {/* Danger Zone */}
                    <div className="danger-zone">
                        <button onClick={handleWithdrawal} className="btn-withdraw">
                            회원 탈퇴하기
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
