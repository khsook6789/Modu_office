import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import Input from '../../components/Input';
import { authApi } from './api/auth.api';
import './AuthStyles.css';

export default function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const response = await authApi.login({ email, password });
            const userData = {
                id: String(response.user.id),
                name: response.user.name,
                email: response.user.email,
                role: response.user.role
            };
            login(userData, response.accessToken);
            if (response.refreshToken) {
                localStorage.setItem('refreshToken', response.refreshToken);
            }
            navigate('/rooms');
        } catch (err: any) {
            console.error('Login error:', err);
            setError(err.message || '로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-card">
            {/* Logo Area */}
            <div className="auth-logo-area">
                <Link to="/" className="auth-logo-link">
                    <span className="auth-logo-dot" />
                    <span className="auth-logo-text">Modu Office</span>
                </Link>
                <p className="auth-logo-tagline">스마트한 공간 예약 플랫폼</p>
            </div>

            <div className="auth-heading">
                <h2 className="auth-title">로그인</h2>
                <p className="auth-subtitle">계정에 로그인하여 서비스를 이용하세요</p>
            </div>

            {/* Error Message */}
            {error && (
                <div className="auth-error">
                    ⚠ {error}
                </div>
            )}

            {/* Form */}
            <form onSubmit={handleSubmit} className="auth-form">
                <div className="input-wrapper" style={{ width: '100%' }}>
                    <Input
                        label="이메일 주소"
                        type="email"
                        placeholder="example@company.com"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                        fullWidth
                    />
                </div>
                <div className="input-wrapper" style={{ width: '100%' }}>
                    <Input
                        label="비밀번호"
                        type="password"
                        placeholder="비밀번호를 입력하세요"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                        fullWidth
                    />
                </div>
                <button
                    type="submit"
                    className="auth-btn-submit"
                    disabled={loading}
                >
                    {loading ? '로그인 중...' : '로그인'}
                </button>
            </form>

            {/* Footer Links */}
            <div className="auth-footer">
                계정이 없으신가요?
                <Link to="/signup" className="auth-footer-link">회원가입</Link>
            </div>
            
            <Link to="/admin/signup" className="auth-admin-link">관리자 회원가입 (테스트용)</Link>
        </div>
    );
}
