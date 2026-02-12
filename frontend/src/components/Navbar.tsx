import { Link, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import './Navbar.css';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [theme, setTheme] = useState(() => localStorage.getItem('theme') || 'dark');

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = async () => {
    const nextTheme = theme === 'dark' ? 'light' : 'dark';

    // View Transitions API support
    if (!document.startViewTransition) {
      setTheme(nextTheme);
      return;
    }

    // Capture the click position (or center of button if triggered otherwise)
    const button = document.querySelector('.theme-toggle-btn');
    const rect = button?.getBoundingClientRect();
    const x = rect ? rect.left + rect.width / 2 : window.innerWidth / 2;
    const y = rect ? rect.top + rect.height / 2 : window.innerHeight / 2;
    const endRadius = Math.hypot(
      Math.max(x, window.innerWidth - x),
      Math.max(y, window.innerHeight - y)
    );

    const transition = document.startViewTransition(() => {
      setTheme(nextTheme);
    });

    await transition.ready;

    // Animate the circular clip path
    document.documentElement.animate(
      {
        clipPath: [
          `circle(0px at ${x}px ${y}px)`,
          `circle(${endRadius}px at ${x}px ${y}px)`,
        ],
      },
      {
        duration: 500,
        easing: 'ease-in',
        pseudoElement: '::view-transition-new(root)',
      }
    );
  };

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
          <button
            onClick={toggleTheme}
            className="btn-icon theme-toggle-btn"
            aria-label="Toggle theme"
            title={theme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'}
          >
            {theme === 'dark' ? (
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="5"></circle>
                <line x1="12" y1="1" x2="12" y2="3"></line>
                <line x1="12" y1="21" x2="12" y2="23"></line>
                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
                <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
                <line x1="1" y1="12" x2="3" y2="12"></line>
                <line x1="21" y1="12" x2="23" y2="12"></line>
                <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
                <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
              </svg>
            ) : (
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
              </svg>
            )}
          </button>
          {user ? (
            <div className="flex-center gap-md">
              {/* Common Links */}
              <Link to="/community" className="nav-link">커뮤니티</Link>
              <Link to="/my-bookings" className="nav-link">내 예약</Link>

              {/* Role Based Links */}
              {(user.role === 'OPERATOR' || user.role === 'ADMIN') && (
                <Link to="/operator" className="nav-link font-bold text-primary">운영자 대시보드</Link>
              )}

              {user.role === 'ADMIN' && (
                <Link to="/admin" className="nav-link text-accent font-bold">관리자</Link>
              )}

              <div className="separator mx-sm h-4 border-l border-gray-300"></div>

              {/* User Profile Area */}
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
