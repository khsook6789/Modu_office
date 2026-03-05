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
                {user ? '스마트 예약 시작하기' : '무료로 시작하기 🚀'}
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
                {/* Detailed Realistic Dashboard Mockup */}
                <div className="mockup-body" style={{ padding: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column', height: '100%' }}>
                    
                    {/* Mockup Top Navbar */}
                    <div style={{ padding: '1rem 1.5rem', borderBottom: '1px solid rgba(0,0,0,0.04)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#ffffff' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                            <div style={{ width: 28, height: 28, borderRadius: 6, background: 'linear-gradient(135deg, var(--color-primary), #3b82f6)' }}></div>
                            <span style={{ fontWeight: 800, color: '#0f172a', fontSize: '1.05rem', letterSpacing: '-0.02em' }}>Admin Portal</span>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                            <div style={{ width: 160, height: 32, borderRadius: 16, background: '#f1f5f9', border: '1px solid #e2e8f0' }}></div>
                            <div style={{ width: 32, height: 32, borderRadius: '50%', background: '#cbd5e1' }}></div>
                        </div>
                    </div>

                    {/* Mockup Main Content */}
                    <div style={{ display: 'flex', flex: 1, background: '#f8fafc' }}>
                        
                        {/* Mockup Sidebar */}
                        <div style={{ width: '200px', borderRight: '1px solid rgba(0,0,0,0.04)', padding: '1.5rem 1rem', display: 'flex', flexDirection: 'column', gap: '0.75rem', background: '#ffffff' }}>
                            <div style={{ height: 32, borderRadius: 6, background: 'var(--color-primary-glow)', border: '1px solid var(--color-primary)', display: 'flex', alignItems: 'center', padding: '0 0.75rem' }}>
                                <div style={{ width: '60%', height: 8, borderRadius: 4, background: 'var(--color-primary)' }}></div>
                            </div>
                            <div style={{ height: 32, borderRadius: 6, display: 'flex', alignItems: 'center', padding: '0 0.75rem' }}>
                                <div style={{ width: '40%', height: 8, borderRadius: 4, background: '#cbd5e1' }}></div>
                            </div>
                            <div style={{ height: 32, borderRadius: 6, display: 'flex', alignItems: 'center', padding: '0 0.75rem' }}>
                                <div style={{ width: '70%', height: 8, borderRadius: 4, background: '#cbd5e1' }}></div>
                            </div>
                        </div>

                        {/* Mockup Content Area */}
                        <div style={{ flex: 1, padding: '2rem', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                            
                            {/* Page Header */}
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '0.5rem' }}>
                                <div>
                                    <h3 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#0f172a', margin: '0 0 0.25rem 0' }}>Dashboard Analytics</h3>
                                    <p style={{ color: '#64748b', fontSize: '0.9rem', margin: 0 }}>실시간 오피스 점유율 및 예약 현황</p>
                                </div>
                                <div style={{ width: 100, height: 36, borderRadius: 8, background: 'var(--color-primary)', opacity: 0.9 }}></div>
                            </div>

                            {/* Stats Cards */}
                            <div style={{ display: 'flex', gap: '1rem' }}>
                                {[
                                    { color: '#eff6ff', bar: 'var(--color-primary)' },
                                    { color: '#f0fdf4', bar: '#10b981' },
                                    { color: '#fff7ed', bar: '#f59e0b' }
                                ].map((stat, i) => (
                                    <div key={i} style={{ flex: 1, background: '#ffffff', borderRadius: 12, padding: '1rem', border: '1px solid rgba(0,0,0,0.04)', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.02)' }}>
                                        <div style={{ width: 32, height: 32, borderRadius: 8, background: stat.color, marginBottom: '1rem' }}></div>
                                        <div style={{ width: '80%', height: 20, borderRadius: 4, background: '#1e293b', marginBottom: '0.5rem' }}></div>
                                        <div style={{ width: '50%', height: 10, borderRadius: 4, background: '#94a3b8' }}></div>
                                    </div>
                                ))}
                            </div>

                            {/* Main Chart Card */}
                            <div style={{ flex: 1, background: '#ffffff', borderRadius: 12, padding: '1.5rem', border: '1px solid rgba(0,0,0,0.04)', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.02)', display: 'flex', flexDirection: 'column' }}>
                                <div style={{ width: '30%', height: 16, borderRadius: 4, background: '#e2e8f0', marginBottom: '2rem' }}></div>
                                <div style={{ flex: 1, display: 'flex', alignItems: 'flex-end', gap: '1rem' }}>
                                    {[40, 70, 100, 60, 80, 50, 90].map((h, i) => (
                                        <div key={i} style={{ flex: 1, height: `${h}%`, background: 'var(--color-primary)', opacity: h === 100 ? 1 : 0.4, borderRadius: '4px 4px 0 0' }}></div>
                                    ))}
                                </div>
                            </div>

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
                <span className="feature-icon-large">
                    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" style={{ color: 'var(--color-primary)' }}>
                        <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                        <line x1="16" y1="2" x2="16" y2="6"></line>
                        <line x1="8" y1="2" x2="8" y2="6"></line>
                        <line x1="3" y1="10" x2="21" y2="10"></line>
                        <rect x="8" y="14" width="3" height="3" rx="0.5"></rect>
                    </svg>
                </span>
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
             <div className="feature-visual-box" style={{ background: 'linear-gradient(135deg, #f8fafc, #eff6ff)' }}>
                <span className="feature-icon-large">
                    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" style={{ color: '#10b981' }}>
                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                        <path d="M9 12l2 2 4-4"></path>
                    </svg>
                </span>
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
