import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
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
  const { user } = useAuth();

  return (
    <div className="landing-page">
      
      {/* Hero Section */}
      <section className="hero-section">
        <div className="landing-container hero-content">
          <AnimatedSection>
            <div className="hero-badge">모두를 위한 완벽한 업무 공간</div>
            <h1 className="hero-title">
              <span className="hero-highlight">공간 예약이라면</span>
              더 쉬워야 하니까
            </h1>
          </AnimatedSection>
          
          <AnimatedSection delay={200}>
            <p className="hero-subtitle">
              회의실부터 대형 라운지까지. Modu Office에서 복잡한 절차 없이<br/>
              직관적인 터치 한 번으로 최적의 업무 환경을 구성하세요.
            </p>
          </AnimatedSection>

          <AnimatedSection delay={400}>
            <div className="hero-buttons">
              <button 
                onClick={() => navigate(user ? '/rooms' : '/signup')}
                className="btn-hero-primary"
              >
                {user ? '스마트 예약 시작하기' : '무료로 시작하기'}
              </button>
              {!user && (
                <button 
                  onClick={() => navigate('/login')}
                  className="btn-hero-secondary"
                >
                  로그인
                </button>
              )}
            </div>
          </AnimatedSection>

          {/* Hero Dashboard Image Mockup */}
          <AnimatedSection delay={600} className="w-100 flex-center">
            <div className="hero-mockup-wrapper">
                <div className="dashboard-mockup">
                {/* Fake Browser Header */}
                <div className="mockup-header">
                    <div className="window-dot dot-red"></div>
                    <div className="window-dot dot-yellow"></div>
                    <div className="window-dot dot-green"></div>
                </div>
                {/* Placeholder for Dashboard Content */}
                <div className="mockup-body">
                    <div className="mockup-placeholder">
                        <div style={{ fontSize: '4rem', marginBottom: '0.5rem', filter: 'drop-shadow(0 10px 15px rgba(0,0,0,0.1))' }}>✨</div>
                        <h3 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#0f172a' }}>Premium UX/UI</h3>
                        <p style={{ color: '#64748b', fontSize: '1rem', fontWeight: 500 }}>직관적인 대시보드로 공간을 손쉽게 관리하세요</p>
                        <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
                            <div className="mockup-skeleton-card"></div>
                            <div className="mockup-skeleton-card"></div>
                        </div>
                    </div>
                </div>
                </div>
            </div>
          </AnimatedSection>
        </div>
      </section>

      {/* Stats Section */}
      <section className="stats-section">
        <div className="landing-container stats-grid">
          <AnimatedSection delay={100} className="stat-item">
            <div className="stat-number">306+</div>
            <div className="stat-label">등록된 공간 수</div>
          </AnimatedSection>
          <AnimatedSection delay={300} className="stat-item">
              <div className="stat-number">2.2K</div>
              <div className="stat-label">월간 활성 이용자</div>
          </AnimatedSection>
          <AnimatedSection delay={500} className="stat-item">
              <div className="stat-number">37K+</div>
              <div className="stat-label">총 누적 예약 완료</div>
          </AnimatedSection>
        </div>
      </section>

      {/* Features Section 1 (Image Left, Text Right) */}
      <section className="feature-section">
        <div className="landing-container feature-grid">
          <AnimatedSection className="feature-visual">
             <div className="feature-visual-box">
                <span className="feature-icon-large">📅</span>
             </div>
          </AnimatedSection>
          <AnimatedSection className="feature-content" delay={200}>
            <span className="feature-tag blue">Smart Scheduling</span>
            <h2 className="feature-title">
              복잡한 일정 충돌,<br/>
              이제는 안녕.
            </h2>
            <p className="feature-desc">
              강력한 동시성 제어 엔진(Optimistic Locking)을 통해 사용자 폭주 상황에서도
              중복 예약을 완벽하게 방지합니다. 0.1초의 오차도 허용하지 않는 신뢰성 높은 예약 시스템을 경험하세요.
            </p>
            <ul className="feature-list">
               <li><span className="feature-check">✓</span> 0.1초 이내 충돌 원천 차단</li>
               <li><span className="feature-check">✓</span> 실시간 점유율 동기화</li>
               <li><span className="feature-check">✓</span> 부서별 권한 완벽 분리</li>
            </ul>
          </AnimatedSection>
        </div>
      </section>

      {/* Features Section 2 (Text Left, Image Right) */}
      <section className="feature-section bg-white">
        <div className="landing-container feature-grid reverse">
          <AnimatedSection className="feature-visual">
             <div className="feature-visual-box" style={{ background: 'linear-gradient(135deg, #f0fdf4, #dcfce7)' }}>
                <span className="feature-icon-large">🔒</span>
             </div>
          </AnimatedSection>
          <AnimatedSection className="feature-content" delay={200}>
            <span className="feature-tag green">Secure & Clear</span>
            <h2 className="feature-title">
              투명한 기록과<br/>
              철저한 데이터 보호
            </h2>
            <p className="feature-desc">
              모든 트랜잭션과 예약 변경 로그는 Audit Log에 안전하게 적재됩니다.
              누가 언제 공간을 수정했는지 한눈에 파악하여, 분쟁의 여지 없이 깔끔하게 관리하세요.
            </p>
            <ul className="feature-list">
               <li><span className="feature-check">✓</span> 전체 예약 위/변조 방지</li>
               <li><span className="feature-check">✓</span> JSONB 기반 정밀 로그 트래킹</li>
               <li><span className="feature-check">✓</span> 세밀한 역할 기반 접근 통제(RBAC)</li>
            </ul>
          </AnimatedSection>
        </div>
      </section>

      {/* CTA Section */}
      <section className="cta-section">
         <div className="landing-container cta-content">
            <AnimatedSection>
               <h2 className="cta-title">최고의 효율을 앞당기세요</h2>
               <p className="cta-subtitle">
                  공간 예약의 혁신, 결제 등록 없이 지금 바로 체험이 가능합니다.
               </p>
               <button 
                onClick={() => navigate(user ? '/rooms' : '/signup')} 
                className="btn-cta"
               >
                  {user ? '공간 둘러보기' : '지금 무료 가입하기'}
               </button>
            </AnimatedSection>
         </div>
      </section>

    </div>
  );
}
