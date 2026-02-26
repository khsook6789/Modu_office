import { useState, useEffect } from 'react';
import { adminApi, type AdminUser, type PendingManager } from './api/admin.api';
import './AdminDashboard.css';

type Tab = 'OVERVIEW' | 'USERS' | 'MANAGERS';

export default function AdminDashboard() {
    const [users, setUsers] = useState<AdminUser[]>([]);
    const [pendingManagers, setPendingManagers] = useState<PendingManager[]>([]);
    const [activeTab, setActiveTab] = useState<Tab>('OVERVIEW');
    const [loading, setLoading] = useState(false);

    useEffect(() => { loadData(); }, []);

    const loadData = async () => {
        setLoading(true);
        try {
            const [usersData, managersData] = await Promise.all([
                adminApi.getAllUsers(),
                adminApi.getPendingManagers(),
            ]);
            setUsers(usersData);
            setPendingManagers(managersData);
        } catch (err) {
            console.error('Failed to load admin data', err);
        } finally {
            setLoading(false);
        }
    };

    const handleSuspend = async (user: AdminUser) => {
        if (!window.confirm(`"${user.name}" 계정을 정지하시겠습니까?`)) return;
        try {
            const updated = await adminApi.suspendUser(user.id);
            setUsers(prev => prev.map(u => u.id === updated.id ? updated : u));
        } catch { alert('계정 정지에 실패했습니다.'); }
    };

    const handleReactivate = async (user: AdminUser) => {
        if (!window.confirm(`"${user.name}" 계정 정지를 해제하시겠습니까?`)) return;
        try {
            const updated = await adminApi.reactivateUser(user.id);
            setUsers(prev => prev.map(u => u.id === updated.id ? updated : u));
        } catch { alert('계정 복원에 실패했습니다.'); }
    };

    const handleApprove = async (manager: PendingManager) => {
        if (!window.confirm(`"${manager.name}" MANAGER를 승인하시겠습니까?`)) return;
        try {
            await adminApi.approveManager(manager.userId);
            setPendingManagers(prev => prev.filter(m => m.userId !== manager.userId));
            const usersData = await adminApi.getAllUsers();
            setUsers(usersData);
            alert('승인되었습니다.');
        } catch { alert('승인에 실패했습니다.'); }
    };

    const stats = {
        total: users.length,
        managers: users.filter(u => u.role === 'MANAGER').length,
        suspended: users.filter(u => u.accountStatus === 'SUSPENDED').length,
        pending: pendingManagers.length,
    };

    const TABS: { key: Tab; label: string }[] = [
        { key: 'OVERVIEW', label: '📊 개요' },
        { key: 'USERS', label: `👤 사용자 (${users.length})` },
        { key: 'MANAGERS', label: `✅ 승인 대기 (${pendingManagers.length})` },
    ];

    const STAT_CARDS = [
        { icon: '👥', label: '총 사용자', value: stats.total, cls: 'blue' },
        { icon: '🏢', label: 'MANAGER', value: stats.managers, cls: 'purple' },
        { icon: '🚫', label: '정지 계정', value: stats.suspended, cls: 'red' },
        { icon: '⏳', label: '승인 대기', value: stats.pending, cls: 'amber' },
    ];

    return (
        <div className="admin-dashboard">
            {/* Header */}
            <div className="admin-header">
                <h1 className="admin-title">관리자 <span>대시보드</span></h1>
                <button onClick={loadData} className="btn btn-secondary" style={{ fontSize: '0.85rem' }}>
                    🔄 새로고침
                </button>
            </div>

            {/* Stats */}
            <div className="admin-stats">
                {STAT_CARDS.map(s => (
                    <div className="stat-card" key={s.label}>
                        <div className={`stat-icon ${s.cls}`}>{s.icon}</div>
                        <div>
                            <div className="stat-value">{s.value}</div>
                            <div className="stat-label">{s.label}</div>
                        </div>
                    </div>
                ))}
            </div>

            {/* Tabs */}
            <div className="admin-tabs">
                {TABS.map(t => (
                    <button
                        key={t.key}
                        className={`admin-tab-btn ${activeTab === t.key ? 'active' : ''}`}
                        onClick={() => setActiveTab(t.key)}
                    >
                        {t.label}
                    </button>
                ))}
            </div>

            {loading ? (
                <div className="empty-state"><p>로딩 중...</p></div>
            ) : (
                <>
                    {/* 개요 */}
                    {activeTab === 'OVERVIEW' && (
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.25rem' }}>
                            <div className="admin-table-wrap" style={{ padding: '1.5rem' }}>
                                <h3 style={{ fontWeight: 700, marginBottom: '1rem', color: '#334155' }}>역할 분포</h3>
                                {(['USER', 'MANAGER', 'ADMIN'] as const).map(role => {
                                    const count = users.filter(u => u.role === role).length;
                                    const pct = users.length > 0 ? Math.round((count / users.length) * 100) : 0;
                                    return (
                                        <div key={role} style={{ marginBottom: '0.875rem' }}>
                                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '0.25rem' }}>
                                                <span><span className={`badge-role ${role}`}>{role}</span></span>
                                                <span style={{ color: '#64748b', fontWeight: 600 }}>{count}명 ({pct}%)</span>
                                            </div>
                                            <div style={{ background: '#f1f5f9', borderRadius: '9999px', height: '6px' }}>
                                                <div style={{ width: `${pct}%`, height: '100%', borderRadius: '9999px', background: role === 'ADMIN' ? '#7c3aed' : role === 'MANAGER' ? '#1d4ed8' : '#16a34a', transition: 'width 0.5s' }} />
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                            <div className="admin-table-wrap" style={{ padding: '1.5rem' }}>
                                <h3 style={{ fontWeight: 700, marginBottom: '1rem', color: '#334155' }}>계정 상태 현황</h3>
                                {(['ACTIVE', 'SUSPENDED', 'DELETED'] as const).map(status => {
                                    const count = users.filter(u => u.accountStatus === status).length;
                                    const label = { ACTIVE: '정상', SUSPENDED: '정지', DELETED: '탈퇴' }[status];
                                    return (
                                        <div key={status} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.625rem 0', borderBottom: '1px solid #f1f5f9' }}>
                                            <span className={`badge-status ${status}`}>{label}</span>
                                            <span style={{ fontWeight: 700, fontSize: '1.25rem', color: '#0f172a' }}>{count}명</span>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    )}

                    {/* 사용자 관리 */}
                    {activeTab === 'USERS' && (
                        <div className="admin-table-wrap">
                            <table className="admin-table">
                                <thead>
                                    <tr>
                                        <th>이름</th><th>이메일</th><th>권한</th><th>상태</th><th>가입일</th><th style={{ textAlign: 'center' }}>관리</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {users.length === 0 && (
                                        <tr><td colSpan={6} className="empty-state"><p>사용자가 없습니다.</p></td></tr>
                                    )}
                                    {users.map(u => (
                                        <tr key={u.id}>
                                            <td style={{ fontWeight: 600, color: '#0f172a' }}>{u.name}</td>
                                            <td style={{ color: '#64748b' }}>{u.email}</td>
                                            <td><span className={`badge-role ${u.role}`}>{u.role}</span></td>
                                            <td>
                                                <span className={`badge-status ${u.accountStatus}`}>
                                                    {{ ACTIVE: '정상', SUSPENDED: '정지', DELETED: '탈퇴' }[u.accountStatus] || u.accountStatus}
                                                </span>
                                            </td>
                                            <td style={{ color: '#94a3b8', fontSize: '0.82rem' }}>{new Date(u.createdAt).toLocaleDateString('ko-KR')}</td>
                                            <td style={{ textAlign: 'center' }}>
                                                {u.role !== 'ADMIN' && (
                                                    u.accountStatus === 'ACTIVE'
                                                        ? <button className="btn-suspend" onClick={() => handleSuspend(u)}>정지</button>
                                                        : u.accountStatus === 'SUSPENDED'
                                                            ? <button className="btn-restore" onClick={() => handleReactivate(u)}>복원</button>
                                                            : null
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {/* 승인 대기 */}
                    {activeTab === 'MANAGERS' && (
                        <div className="admin-table-wrap">
                            {pendingManagers.length === 0 ? (
                                <div className="empty-state"><p>🎉 승인 대기 중인 MANAGER가 없습니다.</p></div>
                            ) : (
                                <table className="admin-table">
                                    <thead>
                                        <tr>
                                            <th>이름</th><th>이메일</th><th>신청일</th><th style={{ textAlign: 'center' }}>승인</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {pendingManagers.map(m => (
                                            <tr key={m.userId}>
                                                <td style={{ fontWeight: 600 }}>{m.name}</td>
                                                <td style={{ color: '#64748b' }}>{m.email}</td>
                                                <td style={{ color: '#94a3b8', fontSize: '0.82rem' }}>{new Date(m.createdAt).toLocaleDateString('ko-KR')}</td>
                                                <td style={{ textAlign: 'center' }}>
                                                    <button className="btn-approve" onClick={() => handleApprove(m)}>승인하기</button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            )}
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
