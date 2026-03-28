import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import Input from '../../components/Input';
import { useToast } from '../../components/Toast';
import './AuthStyles.css';

export default function AdminSignupPage() {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [adminCode, setAdminCode] = useState('');
    const [loading, setLoading] = useState(false);
    const { login } = useAuth();
    const navigate = useNavigate();
    const toast = useToast();

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setLoading(true);

        // Security check
        if (adminCode !== 'admin1234') {
            toast.error('관리자 인증 코드가 올바르지 않습니다.');
            setLoading(false);
            return;
        }

        try {
            await new Promise(resolve => setTimeout(resolve, 1000));

            const users = JSON.parse(localStorage.getItem('users') || '[]');
            const existingUser = users.find((u: any) => u.email === email);

            if (existingUser) {
                toast.error('이미 가입된 이메일 주소입니다.');
                setLoading(false);
                return;
            }

            const newUser = { name, email, password, role: 'ADMIN' };
            users.push(newUser);
            localStorage.setItem('users', JSON.stringify(users));

            login({
                id: email,
                name: name,
                email: email,
                role: 'ADMIN'
            }, 'mock-jwt-token-admin');
            navigate('/rooms');
        } catch (error) {
            console.error(error);
            setLoading(false);
        }
    };

    return (
        <div className="auth-card" style={{ borderColor: 'var(--color-accent)' }}>
            <div className="auth-heading text-center">
                <h2 className="auth-title" style={{ color: 'var(--color-accent)' }}>관리자 가입</h2>
                <p className="auth-subtitle">회사 관리자 계정을 생성합니다</p>
            </div>

            <form onSubmit={handleSubmit} className="auth-form">
                <div className="input-wrapper" style={{ width: '100%' }}>
                    <Input
                        label="관리자 이름"
                        type="text"
                        placeholder="관리자 홍길동"
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
                        placeholder="admin@company.com"
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

                <div className="input-wrapper" style={{ width: '100%' }}>
                    <Input
                        label="인증 코드"
                        type="password"
                        placeholder="관리자 인증 코드를 입력하세요"
                        value={adminCode}
                        onChange={(e) => setAdminCode(e.target.value)}
                        required
                        fullWidth
                    />
                </div>

                <button
                    type="submit"
                    className="auth-btn-submit"
                    disabled={loading}
                    style={{ background: 'linear-gradient(135deg, var(--color-accent), #818cf8)' }}
                >
                    {loading ? '인증 및 가입 중...' : '관리자 가입하기'}
                </button>
            </form>

            <div className="auth-footer" style={{ textAlign: 'center' }}>
                <Link to="/login" className="auth-footer-link" style={{ marginLeft: 0 }}>
                    ← 로그인 화면으로 돌아가기
                </Link>
            </div>
        </div>
    );
}
