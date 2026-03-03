import { Outlet } from 'react-router-dom';
import './AuthLayout.css';

export default function AuthLayout() {
  return (
    <div className="auth-layout">
      {/* Left Side - Form Area (라이트 흰색) */}
      <div className="auth-content flex-center">
        <div className="auth-container animate-fade-in">
          <Outlet />
        </div>
      </div>

      {/* Right Side - Hero Image Area */}
      <div className="auth-hero">
        <div className="hero-content">
          <h1 className="brand-title">Modu Office</h1>
          <p className="brand-subtitle">
            효율적인 공간 관리의 시작.<br />
            모두의 오피스와 함께하세요.
          </p>

          {/* SparkPlus 스타일 - 통계 배지 */}
          <div className="hero-stats">
            <div className="hero-stat">
              <span className="hero-stat-value">50+</span>
              <span className="hero-stat-label">입주 가능 지점</span>
            </div>
            <div className="hero-stat-divider" />
            <div className="hero-stat">
              <span className="hero-stat-value">1,200+</span>
              <span className="hero-stat-label">입주 기업</span>
            </div>
            <div className="hero-stat-divider" />
            <div className="hero-stat">
              <span className="hero-stat-value">98%</span>
              <span className="hero-stat-label">고객 만족도</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
