import { useNavigate } from 'react-router-dom';
import { useScrollAnimation } from '../../hooks/useScrollAnimation';
import '../../styles/animations.css';
import './LandingPage.css';

// Reusable animated section component
function AnimatedSection({ children, className = '', delay = 0 }: { children: React.ReactNode, className?: string, delay?: number }) {
  const { ref, isVisible } = useScrollAnimation();
  return (
    <div 
      ref={ref} 
      className={`animate-on-scroll ${isVisible ? 'animate-fade-in-up' : ''} ${className}`}
      style={{ animationDelay: `${delay}ms` }}
    >
      {children}
    </div>
  );
}

export default function LandingPage() {
  const navigate = useNavigate();

  return (
    <div className="landing-page">
      
      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-content">
          <AnimatedSection>
            <h1 className="hero-title">
              <span className="hero-highlight">공간 예약이라면</span>
              더 쉬워야 하니까
            </h1>
          </AnimatedSection>
          
          <AnimatedSection delay={200}>
            <p className="hero-subtitle">
              회의실부터 공유 오피스까지.<br/>
              Modu Office에서 터치 한 번으로 시작하세요.
            </p>
          </AnimatedSection>

          <AnimatedSection delay={400}>
            <div className="hero-buttons">
              <button 
                onClick={() => navigate('/signup')}
                className="btn btn-primary btn-landing"
              >
                무료로 시작하기
              </button>
              <button 
                onClick={() => navigate('/login')}
                className="btn btn-secondary btn-landing"
              >
                로그인
              </button>
            </div>
          </AnimatedSection>

          {/* Hero Dashboard Image Mockup */}
          <AnimatedSection delay={600} className="w-100 flex-center">
            <div className="dashboard-mockup">
              {/* Fake Browser Header */}
              <div className="mockup-header">
                <div className="window-dot dot-red"></div>
                <div className="window-dot dot-yellow"></div>
                <div className="window-dot dot-green"></div>
                <div className="mockup-bar"></div>
              </div>
              {/* Placeholder for Dashboard Content */}
              <div className="mockup-body">
                 <div className="text-center">
                    <div style={{ fontSize: '4rem', marginBottom: '1rem' }}>🖥️</div>
                    <p style={{ color: '#94a3b8', fontSize: '1.25rem' }}>Modu Office Dashboard Mockup</p>
                 </div>
              </div>
            </div>
          </AnimatedSection>
        </div>
      </section>

      {/* Stats Section */}
      <section className="stats-section">
        <div className="stats-grid">
          <AnimatedSection delay={100}>
            <div className="stat-number">306개</div>
            <div className="stat-label">등록된 공간 수</div>
          </AnimatedSection>
          <AnimatedSection delay={300}>
              <div className="stat-number">2,276명</div>
              <div className="stat-label">누적 이용자</div>
          </AnimatedSection>
          <AnimatedSection delay={500}>
              <div className="stat-number">37,295건</div>
              <div className="stat-label">총 예약 완료</div>
          </AnimatedSection>
        </div>
      </section>

      {/* Features Section 1 (Image Left, Text Right) */}
      <section className="feature-section">
        <div className="feature-container">
          <AnimatedSection className="feature-image">
             <div className="feature-icon-box">
                <span>📅</span>
             </div>
          </AnimatedSection>
          <AnimatedSection className="feature-content" delay={200}>
            <span className="feature-tag">Smart Scheduling</span>
            <h2 className="feature-title">
              복잡한 일정 관리도<br/>
              한눈에 파악하세요
            </h2>
            <p className="feature-desc">
              팀원들의 스케줄과 회의실 예약 현황을 하나의 대시보드에서 관리할 수 있습니다.
              중복 예약 방지는 기본, 스마트한 추천 기능까지 경험해보세요.
            </p>
            <ul className="feature-list">
               <li>
                  <span style={{ color: 'var(--color-success)' }}>✓</span> 실시간 예약 현황 확인
               </li>
               <li>
                  <span style={{ color: 'var(--color-success)' }}>✓</span> 드래그 앤 드롭으로 간편 수정
               </li>
               <li>
                  <span style={{ color: 'var(--color-success)' }}>✓</span> 모바일 알림 연동
               </li>
            </ul>
          </AnimatedSection>
        </div>
      </section>

      {/* Features Section 2 (Text Left, Image Right) */}
      <section className="feature-section bg-gray">
        <div className="feature-container reverse">
          <AnimatedSection className="feature-image">
             <div className="feature-icon-box bg-white-box">
                <span>🔒</span>
             </div>
          </AnimatedSection>
          <AnimatedSection className="feature-content" delay={200}>
            <span className="feature-tag green">Secure & Private</span>
            <h2 className="feature-title">
              철저한 보안으로<br/>
              안심하고 사용하세요
            </h2>
            <p className="feature-desc">
              데이터 암호화와 접근 제어 시스템으로 기업의 소중한 정보를 안전하게 보호합니다.
              외부 손님 초대를 위한 별도의 보안 링크 생성 기능도 제공합니다.
            </p>
            <button className="btn btn-primary" style={{ background: 'none', color: 'var(--color-primary)', boxShadow: 'none', padding: 0 }}>
               보안 기능 더 알아보기 →
            </button>
          </AnimatedSection>
        </div>
      </section>

      {/* CTA Section */}
      <section className="cta-section">
         <div className="cta-container">
            <AnimatedSection>
               <h2 className="cta-title">지금 바로 시작해보세요</h2>
               <p className="cta-subtitle">
                  초기 설정 비용 0원. 언제든지 해지 가능합니다.
               </p>
               <button 
                onClick={() => navigate('/signup')} 
                className="btn btn-secondary btn-landing"
                style={{ backgroundColor: 'white', color: 'var(--color-text-main)', border: 'none' }}
               >
                  무료로 시작하기
               </button>
            </AnimatedSection>
         </div>
      </section>

    </div>
  );
}
