import { useState, useEffect } from 'react';
import { adminApi, type AdminUser, type PendingManager, type UpdateLogResponse } from './api/admin.api';
import { facilityApi, type Facility } from './api/facility.api';
import './AdminDashboard.css';

type Tab = 'OVERVIEW' | 'USERS' | 'MANAGERS' | 'AUDIT_LOG' | 'FACILITIES';

export default function AdminDashboard() {
    const [users, setUsers] = useState<AdminUser[]>([]);
    const [pendingManagers, setPendingManagers] = useState<PendingManager[]>([]);
    const [auditLogs, setAuditLogs] = useState<UpdateLogResponse[]>([]);
    const [facilities, setFacilities] = useState<Facility[]>([]);
    const [auditLogsTotal, setAuditLogsTotal] = useState(0);

    const [activeTab, setActiveTab] = useState<Tab>('OVERVIEW');
    const [loading, setLoading] = useState(false);

    // 시설 폼용
    const [editingFacility, setEditingFacility] = useState<Facility | null>(null);
    const [isFacilityModalOpen, setIsFacilityModalOpen] = useState(false);

    useEffect(() => { loadData(); }, []);

    const loadData = async () => {
        setLoading(true);
        try {
            const [usersData, managersData, logsData, facilitiesData] = await Promise.all([
                adminApi.getAllUsers(),
                adminApi.getPendingManagers(),
                adminApi.getLogs(0, 50), // 최근 50개 로드
                facilityApi.getAllFacilities(),
            ]);
            setUsers(usersData);
            setPendingManagers(managersData);
            setAuditLogs(logsData.content);
            setAuditLogsTotal(logsData.totalElements);
            setFacilities(facilitiesData);
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

    const handleDeleteFacility = async (id: number) => {
        if (!window.confirm('정말 이 시설을 삭제하시겠습니까?')) return;
        try {
            await facilityApi.deleteFacility(id);
            setFacilities(prev => prev.filter(f => f.id !== id));
        } catch (err) {
            alert('시설 삭제에 실패했습니다.');
        }
    };

    const handleFacilitySubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const fd = new FormData(e.currentTarget);
        const req = {
            name: fd.get('name') as string,
            description: fd.get('description') as string,
        };
        try {
            if (editingFacility) {
                const res = await facilityApi.updateFacility(editingFacility.id, req);
                setFacilities(prev => prev.map(f => f.id === res.id ? res : f));
            } else {
                const res = await facilityApi.createFacility(req);
                setFacilities(prev => [...prev, res]);
            }
            setIsFacilityModalOpen(false);
            setEditingFacility(null);
        } catch (err) {
            alert('시설 저장에 실패했습니다.');
        }
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
        { key: 'FACILITIES', label: `🏢 시설 관리 (${facilities.length})` },
        { key: 'AUDIT_LOG', label: `📝 감사 로그 (${auditLogsTotal})` },
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

                    {/* 감사 로그 */}
                    {activeTab === 'AUDIT_LOG' && (
                        <div className="admin-table-wrap">
                            <table className="admin-table" style={{ fontSize: '0.85rem' }}>
                                <thead>
                                    <tr>
                                        <th>발생 시간</th>
                                        <th>액션</th>
                                        <th>작업자</th>
                                        <th>예약 (ID)</th>
                                        <th>변경 데이터 요약</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {auditLogs.length === 0 && (
                                        <tr><td colSpan={5} className="empty-state"><p>감사 로그가 없습니다.</p></td></tr>
                                    )}
                                    {auditLogs.map(log => (
                                        <tr key={log.id}>
                                            <td style={{ color: '#64748b', whiteSpace: 'nowrap' }}>
                                                {new Date(log.occurredAt).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                                            </td>
                                            <td>
                                                <span className="badge-role" style={{ background: log.action === 'CREATE' ? '#dcfce7' : log.action === 'CANCEL' ? '#fee2e2' : '#f1f5f9', color: log.action === 'CREATE' ? '#16a34a' : log.action === 'CANCEL' ? '#ef4444' : '#475569' }}>
                                                    {log.action === 'CREATE' ? '생성' : log.action === 'CANCEL' ? '취소' : log.action === 'UPDATE' ? '수정' : log.action}
                                                </span>
                                            </td>
                                            <td style={{ fontWeight: 600 }}>{log.actorName}</td>
                                            <td>
                                                <div style={{ fontWeight: 600 }}>{log.reservationTitle || '-'}</div>
                                                <div style={{ color: '#94a3b8', fontSize: '0.75rem' }}>ID: {log.reservationId}</div>
                                            </td>
                                            <td style={{ maxWidth: '300px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontFamily: 'monospace', color: '#64748b' }}>
                                                {log.afterData ? JSON.stringify(log.afterData) : '-'}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                    
                    {/* 시설 관리 */}
                    {activeTab === 'FACILITIES' && (
                        <div className="admin-table-wrap">
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', padding: '0 0.5rem' }}>
                                <h3 style={{ fontWeight: 700, color: '#334155', margin: 0 }}>전역 공용 시설 목록</h3>
                                <button className="btn btn-primary" onClick={() => { setEditingFacility(null); setIsFacilityModalOpen(true); }} style={{ padding: '0.6rem 1rem', fontSize: '0.9rem' }}>
                                    + 새 시설 추기
                                </button>
                            </div>
                            <table className="admin-table">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>시설명</th>
                                        <th>설명</th>
                                        <th>등록일</th>
                                        <th style={{ textAlign: 'center' }}>관리</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {facilities.length === 0 && (
                                        <tr><td colSpan={5} className="empty-state"><p>등록된 시설이 없습니다.</p></td></tr>
                                    )}
                                    {facilities.map(f => (
                                        <tr key={f.id}>
                                            <td style={{ color: '#94a3b8' }}>#{f.id}</td>
                                            <td style={{ fontWeight: 600 }}>{f.name}</td>
                                            <td style={{ color: '#64748b' }}>{f.description || '-'}</td>
                                            <td style={{ color: '#94a3b8', fontSize: '0.85rem' }}>{new Date(f.createdAt).toLocaleDateString('ko-KR')}</td>
                                            <td style={{ textAlign: 'center' }}>
                                                <button className="btn-restore" style={{ marginRight: '0.4rem' }} onClick={() => { setEditingFacility(f); setIsFacilityModalOpen(true); }}>수정</button>
                                                <button className="btn-suspend" onClick={() => handleDeleteFacility(f.id)}>삭제</button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </>
            )}

            {/* 시설 관리 모달 */}
            {isFacilityModalOpen && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
                    <div style={{ background: '#fff', padding: '2rem', borderRadius: '1rem', width: '400px', boxShadow: '0 10px 25px rgba(0,0,0,0.1)' }}>
                        <h3 style={{ marginTop: 0, marginBottom: '1.5rem', fontWeight: 700, color: '#0f172a' }}>
                            {editingFacility ? '시설 정보 수정' : '새 시설 추가'}
                        </h3>
                        <form onSubmit={handleFacilitySubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            <div>
                                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '0.4rem' }}>시설명 (아이콘 포함 권장)</label>
                                <input name="name" defaultValue={editingFacility?.name || ''} required placeholder="예: 📶 Wi-Fi" style={{ width: '100%', padding: '0.75rem', border: '1px solid #cbd5e1', borderRadius: '0.5rem' }} />
                            </div>
                            <div>
                                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '0.4rem' }}>상세 설명 (선택)</label>
                                <textarea name="description" defaultValue={editingFacility?.description || ''} placeholder="시설에 대한 부가 설명" rows={3} style={{ width: '100%', padding: '0.75rem', border: '1px solid #cbd5e1', borderRadius: '0.5rem', resize: 'vertical' }} />
                            </div>
                            <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
                                <button type="button" onClick={() => { setIsFacilityModalOpen(false); setEditingFacility(null); }} style={{ flex: 1, padding: '0.75rem', background: '#f1f5f9', color: '#475569', border: 'none', borderRadius: '0.5rem', fontWeight: 600, cursor: 'pointer' }}>취소</button>
                                <button type="submit" style={{ flex: 1, padding: '0.75rem', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '0.5rem', fontWeight: 600, cursor: 'pointer' }}>저장</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
