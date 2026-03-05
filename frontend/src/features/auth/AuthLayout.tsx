import type { ReactNode } from 'react';
import './AuthLayout.css';
import { Link } from 'react-router-dom';

interface AuthLayoutProps {
    children: ReactNode;
    title: string;
    subtitle: string;
}

export default function AuthLayout({ children, title, subtitle }: AuthLayoutProps) {
    return (
        <div className="auth-split-container">
            {/* Left Side: Brand Visuals (Image + Overlay) */}
            <div className="auth-visual-side">
                <div className="auth-visual-overlay">
                    <div className="auth-visual-content">
                        <Link to="/" className="auth-visual-logo">
                            <span className="auth-logo-dot white" />
                            <span className="auth-logo-text white">Modu Office</span>
                        </Link>
                        <h2 className="auth-visual-title">스마트한 오피스의 시작</h2>
                        <p className="auth-visual-desc">
                            복잡한 절차 없이 직관적인 터치 한 번으로<br />
                            최적의 업무 환경을 구성하세요.
                        </p>
                    </div>
                </div>
            </div>

            {/* Right Side: Auth Form */}
            <div className="auth-form-side">
                <div className="auth-form-wrapper">
                    <div className="auth-heading">
                        <h2 className="auth-title">{title}</h2>
                        <p className="auth-subtitle">{subtitle}</p>
                    </div>
                    {children}
                </div>
            </div>
        </div>
    );
}
