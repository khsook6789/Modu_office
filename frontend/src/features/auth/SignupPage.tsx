import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Input from '../../components/Input';
import { authApi } from './api/auth.api';
import { useToast } from '../../components/Toast';
import './AuthStyles.css';

export default function SignupPage() {
    const [userType, setUserType] = useState<'USER' | 'MANAGER'>('USER');
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const navigate = useNavigate();
    const toast = useToast();

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            if (userType === 'USER') {
                await authApi.signup({ name, email, password });
                toast.success('회원가입이 완료되었습니다. 로그인해주세요.');
                navigate('/login');
            } else {
                await authApi.signupOperator({ name, email, password });
                toast.success('파트너(운영자) 가입 신청이 완료되었습니다. 관리자 승인 후 이용 가능합니다.');
                navigate('/login');
            }
        } catch (err: any) {
            console.error('Signup error:', err);
            setError(err.message || '회원가입에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-card wide">
            {/* Logo Area */}
            <div className="auth-logo-area">
                <Link to="/" className="auth-logo-link">
                    <span className="auth-logo-dot" />
                    <span className="auth-logo-text">Modu Office</span>
                </Link>
                <p className="auth-logo-tagline">무료 가입으로 시작해보세요</p>
            </div>

            <div className="auth-heading">
                <h2 className="auth-title">회원가입</h2>
                <p className="auth-subtitle">Modu Office와 함께 새로운 공간 경험을 시작하세요</p>
            </div>

            {/* User Type Toggle */}
            <div className="auth-type-toggle">
                <button
                    type="button"
                    className={`auth-type-btn ${userType === 'USER' ? 'active' : ''}`}
                    onClick={() => setUserType('USER')}
                >
                    일반 회원
                </button>
                <button
                    type="button"
                    className={`auth-type-btn ${userType === 'MANAGER' ? 'active' : ''}`}
                    onClick={() => setUserType('MANAGER')}
                >
                    오피스 운영자
                </button>
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
                        label={userType === 'USER' ? "이름" : "사업자명 (또는 대표자명)"}
                        type="text"
                        placeholder={userType === 'USER' ? "홍길동" : "모두컴퍼니"}
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        required
                        fullWidth
                    />
                </div>

                <div className="input-wrapper" style={{ width: '100%' }}>
                    <Input
                        label="이메일 주소"
                        type="email"
                        placeholder="name@company.com"
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
                        placeholder="비밀번호를 입력해주세요"
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
                    {loading ? '가입 처리 중...' : (userType === 'USER' ? '가입하기' : '파트너 신청')}
                </button>
            </form>

            {/* Footer Links */}
            <div className="auth-footer">
                이미 계정이 있으신가요?
                <Link to="/login" className="auth-footer-link">로그인</Link>
            </div>
        </div>
    );
}
