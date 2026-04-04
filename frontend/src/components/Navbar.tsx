import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import NotificationBell from './NotificationBell';
import './Navbar.css';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="container flex-between h-full">
        <Link to="/" className="brand">
          <span className="text-gradient font-bold text-xl">Modu Office</span>
        </Link>

        <div className="flex-center gap-md">
          {user ? (
            <div className="flex-center gap-md">
              <Link to="/rooms" className="nav-link">공간 예약</Link>
              <Link to="/my-bookings" className="nav-link">내 예약</Link>
              <Link to="/favorites" className="nav-link">❤️ 즐겨찾기</Link>
              {(user.role === 'MANAGER' || user.role === 'ADMIN') && (
                <Link to="/operator" className="nav-link">운영자 대시보드</Link>
              )}
              {user.role === 'ADMIN' && (
                <Link to="/admin" className="nav-link" style={{ color: '#e11d48' }}>관리자</Link>
              )}
              <NotificationBell />
              <div className="separator mx-sm h-4 border-l border-gray-300"></div>
              <div className="flex items-center gap-sm">
                <Link to="/mypage" className="flex items-center gap-xs hover:text-primary transition-colors">
                  <span className="text-sm font-medium">{user.name}님</span>
                </Link>
                <button
                  onClick={handleLogout}
                  className="btn btn-secondary text-xs px-3 py-1 hover:bg-red-500/10 hover:text-red-500 hover:border-red-500/30 transition-all"
                >
                  로그아웃
                </button>
              </div>
            </div>
          ) : (
            <div className="flex gap-sm">
              <Link to="/login" className="btn btn-ghost text-sm">로그인</Link>
              <Link to="/signup" className="btn btn-primary text-sm px-4 py-2 rounded-full">회원가입</Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}
