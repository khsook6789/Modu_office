import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
// import { useAuth } from '../../contexts/AuthContext'; // Removed unused import
import Input from '../../components/Input';
import { authApi } from './api/auth.api';
import './SignupPage.css';

export default function SignupPage() {
    const [userType, setUserType] = useState<'USER' | 'OPERATOR'>('USER');
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setLoading(true);

        try {
            if (userType === 'USER') {
                await authApi.signup({ name, email, password });
                alert('회원가입이 완료되었습니다. 로그인해주세요.');
                navigate('/login');
            } else {
                await authApi.signupOperator({ name, email, password });
                alert('파트너(운영자) 가입 신청이 완료되었습니다. 관리자 승인 후 이용 가능합니다.');
                navigate('/login');
            }
        } catch (error: any) {
            console.error('Signup error:', error);
            alert(error.message || '회원가입에 실패했습니다.');
            setLoading(false);
        }
    };

    return (
        <div className="card signup-card">
            <div className="text-center mb-md">
                <h1 className="signup-title font-bold mb-xs">회원가입</h1>
                <p className="signup-subtitle text-muted text-sm">Modu Office와 함께하세요</p>
            </div>

            {/* User Type Toggle */}
            <div className="flex justify-center mb-md">
                <div className="flex bg-gray-100 p-1 rounded-lg" style={{ background: '#f5f5f5', display: 'inline-flex' }}>
                    <button
                        type="button"
                        className={`px-4 py-2 text-sm rounded-md transition-all ${userType === 'USER'
                            ? 'bg-primary text-white shadow-md'
                            : 'bg-gray-100 text-gray-500 hover:bg-gray-200'}`}
                        onClick={() => setUserType('USER')}
                    >
                        일반 회원
                    </button>
                    <button
                        type="button"
                        className={`px-4 py-2 text-sm rounded-md transition-all ${userType === 'OPERATOR'
                                ? 'bg-white shadow text-primary font-bold'
                                : 'text-muted hover:text-gray-700'
                            }`}
                        onClick={() => setUserType('OPERATOR')}
                    >
                        오피스 운영자
                    </button>
                </div>
            </div>

            <form onSubmit={handleSubmit}>
                <Input
                    label={userType === 'USER' ? "이름" : "대표자명"}
                    type="text"
                    placeholder={userType === 'USER' ? "홍길동" : "사업자명 또는 대표자명"}
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                    fullWidth
                />

                <Input
                    label="이메일 주소"
                    type="email"
                    placeholder="name@company.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    fullWidth
                />

                <Input
                    label="비밀번호"
                    type="password"
                    placeholder="비밀번호를 입력해주세요"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    fullWidth
                />

                <button
                    type="submit"
                    className="btn btn-primary signup-btn w-full mt-md text-md py-3"
                    disabled={loading}
                >
                    {loading ? '가입 처리 중...' : (userType === 'USER' ? '회원가입 하기' : '파트너 가입 신청')}
                </button>
            </form>

            <div className="text-center mt-lg text-sm signup-footer">
                <span className="text-muted">이미 계정이 있으신가요? </span>
                <Link to="/login" className="text-primary hover:underline">
                    로그인
                </Link>
            </div>
        </div>
    );
}
