import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import Input from '../../components/Input';
import { authApi } from './api/auth.api';
import './LoginPage.css';

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
            
            // Map backend response to AuthContext User type
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
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', paddingTop: '3rem' }}>
            <Link to="/" style={{ textDecoration: 'none', marginBottom: '2rem' }}>
                <span className="text-gradient font-bold" style={{ fontSize: '1.75rem', letterSpacing: '-0.5px' }}>Modu Office</span>
            </Link>
        <div className="card login-card" style={{ width: '100%' }}>
            <div className="text-center mb-lg">
                <p className="login-title font-bold mb-sm">환영합니다!</p>
                <p className="login-subtitle text-muted text-sm">서비스 이용을 위해 로그인해주세요.</p>
            </div>

            {error && (
                <div className="alert-error mb-md text-sm p-sm rounded-md">
                    {error}
                </div>
            )}

            <form onSubmit={handleSubmit}>
                <Input
                    label="이메일 주소"
                    type="email"
                    placeholder="이메일을 입력하세요"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    fullWidth
                />

                <Input
                    label="비밀번호"
                    type="password"
                    placeholder="비밀번호를 입력하세요"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    fullWidth
                />

                <button
                    type="submit"
                    className="btn btn-primary login-btn w-full mt-md text-md py-3"
                    disabled={loading}
                >
                    {loading ? '로그인 중...' : '로그인'}
                </button>
            </form>

            <div className="text-center mt-lg text-sm login-footer">
                <span className="text-muted">계정이 없으신가요? </span>
                <Link to="/signup" className="text-primary hover:underline">
                    회원가입
                </Link>
                <div className="mt-sm">
                    <Link to="/admin/signup" className="text-xs text-muted hover:text-primary">
                        관리자 회원가입
                    </Link>
                </div>
            </div>
        </div>
        </div>
    );
}
