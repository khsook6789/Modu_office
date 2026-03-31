import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import NotificationBell from './NotificationBell';
import './Navbar.css';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
    setMenuOpen(false);
  };

  const closeMenu = () => setMenuOpen(false);

  return (
    <nav className="navbar">
      <div className="container flex-between h-full">
        <Link to="/" className="brand" onClick={closeMenu}>
          <span className="text-gradient font-bold text-xl">Modu Office</span>
        </Link>

        {/* 데스크탑 메뉴 */}
        <div className="nav-desktop flex-center gap-md">
          {user ? (
            <div className="flex-center gap-md">
              <Link to="/rooms" className="nav-link">공간 예약</Link>
              <Link to="/my-bookings" className="nav-link">내 예약</Link>
              <Link to="/favorites" className="nav-link">❤️ 즐겨찾기</Link>
              {(user.role === 'MANAGER' || user.role === 'ADMIN') && (
                <Link to="/operator" className="nav-link">운영자 대시보드</Link>
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

        {/* 모바일: 알림 + 햄버거 버튼 */}
        <div className="nav-mobile-actions">
          {user && <NotificationBell />}
          <button
            className="hamburger-btn"
            onClick={() => setMenuOpen(o => !o)}
            aria-label="메뉴 열기"
          >
            <span className={`hamburger-icon ${menuOpen ? 'open' : ''}`}>
              <span /><span /><span />
            </span>
          </button>
        </div>
      </div>

      {/* 모바일 드로어 */}
      {menuOpen && (
        <div className="mobile-menu">
          {user ? (
            <>
              <div className="mobile-menu-user">
                <span className="mobile-user-name">{user.name}님</span>
                <span className="mobile-user-role">{user.role === 'USER' ? '일반 사용자' : user.role === 'MANAGER' ? '운영자' : '관리자'}</span>
              </div>
              <Link to="/rooms" className="mobile-nav-link" onClick={closeMenu}>🏢 공간 예약</Link>
              <Link to="/my-bookings" className="mobile-nav-link" onClick={closeMenu}>📋 내 예약</Link>
              <Link to="/favorites" className="mobile-nav-link" onClick={closeMenu}>❤️ 즐겨찾기</Link>
              <Link to="/mypage" className="mobile-nav-link" onClick={closeMenu}>👤 마이페이지</Link>
              {(user.role === 'MANAGER' || user.role === 'ADMIN') && (
                <Link to="/operator" className="mobile-nav-link" onClick={closeMenu}>⚙️ 운영자 대시보드</Link>
              )}
              <div className="mobile-menu-divider" />
              <button className="mobile-nav-link mobile-logout" onClick={handleLogout}>🚪 로그아웃</button>
            </>
          ) : (
            <>
              <Link to="/login" className="mobile-nav-link" onClick={closeMenu}>로그인</Link>
              <Link to="/signup" className="mobile-nav-link" onClick={closeMenu}>회원가입</Link>
            </>
          )}
        </div>
      )}
    </nav>
  );
}
