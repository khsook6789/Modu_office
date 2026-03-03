import { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { client } from '../../api/client';
import { reviewApi, type Review } from '../reviews/api/review.api';
import './MyPage.css';

type Tab = 'PROFILE' | 'PASSWORD' | 'REVIEWS';

export default function MyPage() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const [activeTab, setActiveTab] = useState<Tab>('PROFILE');

    // 비밀번호 변경
    const [pwForm, setPwForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
    const [pwLoading, setPwLoading] = useState(false);
    const [pwMsg, setPwMsg] = useState('');

    // 내 리뷰
    const [reviews, setReviews] = useState<Review[]>([]);
    const [reviewsLoading, setReviewsLoading] = useState(false);

    useEffect(() => {
        if (activeTab === 'REVIEWS') loadReviews();
    }, [activeTab]);

    const loadReviews = async () => {
        setReviewsLoading(true);
        try {
            const data = await reviewApi.getMyReviews();
            setReviews(data);
        } catch { } finally {
            setReviewsLoading(false);
        }
    };

    const handlePasswordChange = async (e: React.FormEvent) => {
        e.preventDefault();
        setPwMsg('');
        if (pwForm.newPassword !== pwForm.confirmPassword) {
            setPwMsg('새 비밀번호가 일치하지 않습니다.');
            return;
        }
        if (pwForm.newPassword.length < 8) {
            setPwMsg('새 비밀번호는 8자 이상이어야 합니다.');
            return;
        }
        setPwLoading(true);
        try {
            await client.put('/users/me/password', {
                currentPassword: pwForm.currentPassword,
                newPassword: pwForm.newPassword,
            });
            setPwMsg('✅ 비밀번호가 변경되었습니다.');
            setPwForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
        } catch (err: any) {
            const msg = err?.response?.data?.message || '비밀번호 변경에 실패했습니다.';
            setPwMsg(`❌ ${msg}`);
        } finally {
            setPwLoading(false);
        }
    };

    const handleWithdrawal = async () => {
        const pw = window.prompt('탈퇴하려면 현재 비밀번호를 입력하세요:');
        if (!pw) return;
        if (!window.confirm('정말로 탈퇴하시겠습니까? 모든 데이터가 삭제됩니다.')) return;
        try {
            const token = localStorage.getItem('token');
            const res = await fetch('http://localhost:8080/api/users/me', {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    ...(token ? { Authorization: `Bearer ${token}` } : {}),
                },
                body: JSON.stringify({ password: pw }),
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || '탈퇴에 실패했습니다.');
            }
            alert('회원탈퇴가 완료되었습니다.');
            logout();
            navigate('/login');
        } catch (err: any) {
            alert(`❌ ${err.message}`);
        }
    };

    const handleDeleteReview = async (reviewId: number) => {
        if (!window.confirm('리뷰를 삭제하시겠습니까?')) return;
        try {
            await reviewApi.deleteReview(reviewId);
            setReviews(prev => prev.filter(r => r.id !== reviewId));
        } catch { alert('리뷰 삭제에 실패했습니다.'); }
    };

    if (!user) return <div className="mypage-container"><p>로딩 중이거나 로그인이 필요합니다.</p></div>;

    const initial = user.name ? user.name.charAt(0).toUpperCase() : '?';

    const TABS: { key: Tab; label: string }[] = [
        { key: 'PROFILE', label: '👤 내 정보' },
        { key: 'PASSWORD', label: '🔒 비밀번호' },
        { key: 'REVIEWS', label: '💬 내 리뷰' },
    ];

    return (
        <div className="mypage-container">
            <div className="mypage-content">
                <div className="mypage-header">
                    <h1 className="mypage-title">마이<span>페이지</span></h1>
                </div>

                {/* Profile Card */}
                <div className="mypage-card">
                    <div className="profile-header">
                        <div className="profile-avatar">{initial}</div>
                        <div className="profile-info">
                            <h2 className="profile-name">{user.name}</h2>
                            <p className="profile-email">{user.email}</p>
                            <span className={`profile-role-badge ${user.role}`}>
                                {user.role === 'USER' ? '일반 사용자' : user.role === 'MANAGER' ? '운영자' : '관리자'}
                            </span>
                        </div>
                    </div>

                    {/* Quick Links */}
                    <div className="settings-list" style={{ marginBottom: '1.5rem' }}>
                        {user.role === 'USER' && (
                            <button className="settings-btn" onClick={() => navigate('/my-bookings')}>
                                <span>📋 내 예약 내역</span>
                                <span style={{ color: 'var(--color-primary)' }}>→</span>
                            </button>
                        )}
                        <button className="settings-btn" onClick={() => navigate('/favorites')}>
                            <span>❤️ 즐겨찾기 공간</span>
                            <span style={{ color: 'var(--color-primary)' }}>→</span>
                        </button>
                        <button className="settings-btn" onClick={logout} style={{ color: '#ef4444' }}>
                            <span>🚪 로그아웃</span>
                        </button>
                    </div>
                </div>

                {/* Tabs */}
                <div className="mypage-tabs">
                    {TABS.map(t => (
                        <button
                            key={t.key}
                            className={`mypage-tab-btn ${activeTab === t.key ? 'active' : ''}`}
                            onClick={() => setActiveTab(t.key)}
                        >
                            {t.label}
                        </button>
                    ))}
                </div>

                <div className="mypage-card">
                    {/* 내 정보 탭 */}
                    {activeTab === 'PROFILE' && (
                        <div>
                            <h3 className="settings-section-title">기본 정보</h3>
                            <div className="info-item">
                                <span className="info-label">이름</span>
                                <span className="info-value">{user.name}</span>
                            </div>
                            <div className="info-item">
                                <span className="info-label">이메일</span>
                                <span className="info-value">{user.email}</span>
                            </div>
                            <div className="info-item">
                                <span className="info-label">권한</span>
                                <span className={`profile-role-badge ${user.role}`}>
                                    {user.role === 'USER' ? '일반 사용자' : user.role === 'MANAGER' ? '운영자' : '관리자'}
                                </span>
                            </div>
                        </div>
                    )}

                    {/* 비밀번호 탭 */}
                    {activeTab === 'PASSWORD' && (
                        <div>
                            <h3 className="settings-section-title">비밀번호 변경</h3>
                            <form onSubmit={handlePasswordChange} className="pw-form">
                                <div className="form-group">
                                    <label className="form-label">현재 비밀번호</label>
                                    <input
                                        type="password"
                                        className="form-input"
                                        value={pwForm.currentPassword}
                                        onChange={e => setPwForm(p => ({ ...p, currentPassword: e.target.value }))}
                                        required
                                        placeholder="현재 비밀번호 입력"
                                    />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">새 비밀번호</label>
                                    <input
                                        type="password"
                                        className="form-input"
                                        value={pwForm.newPassword}
                                        onChange={e => setPwForm(p => ({ ...p, newPassword: e.target.value }))}
                                        required
                                        placeholder="새 비밀번호 (8자 이상)"
                                        minLength={8}
                                    />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">새 비밀번호 확인</label>
                                    <input
                                        type="password"
                                        className="form-input"
                                        value={pwForm.confirmPassword}
                                        onChange={e => setPwForm(p => ({ ...p, confirmPassword: e.target.value }))}
                                        required
                                        placeholder="새 비밀번호 재입력"
                                    />
                                </div>
                                {pwMsg && (
                                    <p style={{ color: pwMsg.startsWith('✅') ? '#16a34a' : '#ef4444', fontWeight: 600, marginBottom: '1rem', fontSize: '0.9rem' }}>
                                        {pwMsg}
                                    </p>
                                )}
                                <button type="submit" className="btn btn-primary" disabled={pwLoading} style={{ width: '100%' }}>
                                    {pwLoading ? '변경 중...' : '비밀번호 변경'}
                                </button>
                            </form>
                        </div>
                    )}

                    {/* 내 리뷰 탭 */}
                    {activeTab === 'REVIEWS' && (
                        <div>
                            <h3 className="settings-section-title">내가 작성한 리뷰 ({reviews.length})</h3>
                            {reviewsLoading ? (
                                <p style={{ color: '#94a3b8', textAlign: 'center', padding: '2rem 0' }}>불러오는 중...</p>
                            ) : reviews.length === 0 ? (
                                <div style={{ textAlign: 'center', padding: '2.5rem 0', color: '#94a3b8' }}>
                                    <p style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>💬</p>
                                    <p>아직 작성한 리뷰가 없습니다.</p>
                                    <Link to="/rooms" style={{ color: 'var(--color-primary)', fontWeight: 700, marginTop: '0.5rem', display: 'inline-block' }}>
                                        회의실 둘러보기 →
                                    </Link>
                                </div>
                            ) : (
                                <div className="review-list">
                                    {reviews.map(r => (
                                        <div key={r.id} className="my-review-item">
                                            <div className="my-review-header">
                                                <span className="review-stars">{'⭐'.repeat(r.rating)}</span>
                                                <span className="review-date">{new Date(r.createdAt).toLocaleDateString('ko-KR')}</span>
                                            </div>
                                            <p className="review-comment">{r.comment}</p>
                                            <button className="review-delete-btn" onClick={() => handleDeleteReview(r.id)}>삭제</button>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}
                </div>

                {/* Danger Zone */}
                <div className="danger-zone">
                    <button onClick={handleWithdrawal} className="btn-withdraw">회원 탈퇴하기</button>
                </div>
            </div>
        </div>
    );
}
