import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { officeApi, type Office } from '../rooms/api/office.api';
import { roomApi } from '../rooms/api/room.api';
import { bookingApi, type Booking } from '../booking/api/booking.api';
import './OperatorDashboard.css';

type Tab = 'OFFICES' | 'RESERVATIONS';

const STAT_CARDS = (stats: { offices: number; rooms: number; capacity: number; pending: number }) => [
    { icon: '🏢', label: '운영 중인 오피스', value: stats.offices, unit: '개', cls: 'blue' },
    { icon: '🚪', label: '총 회의실 수', value: stats.rooms, unit: '개', cls: 'teal' },
    { icon: '👥', label: '총 수용 인원', value: stats.capacity, unit: '명', cls: 'orange' },
    { icon: '⏳', label: '승인 대기 예약', value: stats.pending, unit: '건', cls: 'yellow' },
];

export default function OperatorDashboard() {
    const navigate = useNavigate();
    const [offices, setOffices] = useState<Office[]>([]);
    const [reservations, setReservations] = useState<Booking[]>([]);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState<Tab>('OFFICES');
    const [stats, setStats] = useState({ offices: 0, rooms: 0, capacity: 0, pending: 0 });

    useEffect(() => { loadOffices(); }, []);

    const loadOffices = async () => {
        try {
            setLoading(true);
            const [officesData, roomsData] = await Promise.all([
                officeApi.getMyOffices(),
                roomApi.getAllRooms(),
            ]);
            setOffices(officesData);
            setStats(prev => ({
                ...prev,
                offices: officesData.length,
                rooms: roomsData.length,
                capacity: roomsData.reduce((a, r) => a + r.capacity, 0),
            }));
        } catch (e) { console.error(e); }
        finally { setLoading(false); }
    };

    const loadReservations = async () => {
        try {
            setLoading(true);
            const data = await bookingApi.getMyBookings();
            const list: Booking[] = Array.isArray(data) ? data : (data as any).content ?? [];
            setReservations(list);
            setStats(prev => ({ ...prev, pending: list.filter(r => r.status === 'PENDING').length }));
        } catch (e) { console.error(e); }
        finally { setLoading(false); }
    };

    const handleTabChange = (tab: Tab) => {
        setActiveTab(tab);
        if (tab === 'RESERVATIONS') loadReservations();
    };

    const handleDeleteOffice = async (office: Office) => {
        if (!window.confirm(`"${office.name}" 오피스를 삭제하시겠습니까?`)) return;
        try {
            await officeApi.deleteOffice(office.id);
            setOffices(prev => prev.filter(o => o.id !== office.id));
        } catch { alert('삭제에 실패했습니다.'); }
    };

    const handleConfirm = async (id: number) => {
        if (!window.confirm('이 예약을 확정하시겠습니까?')) return;
        try {
            await bookingApi.confirmBooking(id);
            setReservations(prev => prev.map(r => r.id === id ? { ...r, status: 'CONFIRMED' as const } : r));
        } catch { alert('확정에 실패했습니다.'); }
    };

    const handleCancelRes = async (id: number) => {
        if (!window.confirm('이 예약을 취소하시겠습니까?')) return;
        try {
            await bookingApi.cancelBooking(id);
            setReservations(prev => prev.map(r => r.id === id ? { ...r, status: 'CANCELLED' as const } : r));
        } catch { alert('취소에 실패했습니다.'); }
    };

    const statusLabel = (s: string) => ({ PENDING: '대기', CONFIRMED: '확정', CANCELLED: '취소' }[s] || s);

    return (
        <div className="operator-dashboard">
            {/* Header */}
            <div className="operator-header">
                <h1 className="operator-title">운영자 <span>대시보드</span></h1>
            </div>

            {/* Stats */}
            <div className="operator-stats">
                {STAT_CARDS(stats).map(s => (
                    <div className="op-stat-card" key={s.label}>
                        <div className={`op-stat-icon ${s.cls}`}>{s.icon}</div>
                        <div>
                            <div className="op-stat-value">{s.value}<span style={{ fontSize: '1rem', fontWeight: 600, color: '#64748b' }}>{s.unit}</span></div>
                            <div className="op-stat-label">{s.label}</div>
                        </div>
                    </div>
                ))}
            </div>

            {/* Tabs */}
            <div className="op-tabs">
                <button className={`op-tab-btn ${activeTab === 'OFFICES' ? 'active' : ''}`} onClick={() => handleTabChange('OFFICES')}>🏢 오피스 관리</button>
                <button className={`op-tab-btn ${activeTab === 'RESERVATIONS' ? 'active' : ''}`} onClick={() => handleTabChange('RESERVATIONS')}>📋 예약 관리</button>
            </div>

            {loading ? (
                <div className="op-empty"><p>로딩 중...</p></div>
            ) : (
                <>
                    {/* 오피스 관리 */}
                    {activeTab === 'OFFICES' && (
                        <>
                            <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem' }}>
                                <button className="op-add-btn" onClick={() => navigate('/office/new')}>
                                    + 새 오피스 등록
                                </button>
                            </div>
                            {offices.length === 0 ? (
                                <div className="op-empty"><p>등록된 오피스가 없습니다.</p></div>
                            ) : (
                                <div className="op-office-list">
                                    {offices.map(o => (
                                        <div className="op-office-card" key={o.id}>
                                            <div>
                                                <div className="op-office-name">{o.name}</div>
                                                <div className="op-office-meta">
                                                    <span>📍 {o.location}</span>
                                                    <span>🕒 {o.openTime} ~ {o.closeTime}</span>
                                                </div>
                                            </div>
                                            <div className="op-office-actions">
                                                <button className="op-btn-rooms" onClick={() => navigate(`/office/${o.id}/manage`)}>회의실 관리</button>
                                                <button className="op-btn-edit" onClick={() => navigate(`/office/${o.id}/edit`)}>수정</button>
                                                <button className="op-btn-delete" onClick={() => handleDeleteOffice(o)}>삭제</button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </>
                    )}

                    {/* 예약 관리 */}
                    {activeTab === 'RESERVATIONS' && (
                        <div className="op-table-wrap">
                            {reservations.length === 0 ? (
                                <div className="op-empty"><p>예약 내역이 없습니다.</p></div>
                            ) : (
                                <table className="op-table">
                                    <thead>
                                        <tr>
                                            <th>예약명</th><th>회의실</th><th>예약자</th><th>시작</th><th>종료</th><th>상태</th><th style={{ textAlign: 'center' }}>관리</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {reservations.map(r => (
                                            <tr key={r.id}>
                                                <td style={{ fontWeight: 600 }}>{r.title}</td>
                                                <td style={{ color: '#64748b' }}>{r.roomName || `#${r.roomId}`}</td>
                                                <td style={{ color: '#64748b' }}>{(r as any).userName || '-'}</td>
                                                <td style={{ color: '#94a3b8', fontSize: '0.82rem' }}>{new Date(r.startAt).toLocaleString('ko-KR', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })}</td>
                                                <td style={{ color: '#94a3b8', fontSize: '0.82rem' }}>{new Date(r.endAt).toLocaleString('ko-KR', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })}</td>
                                                <td><span className={`badge-reservation ${r.status}`}>{statusLabel(r.status)}</span></td>
                                                <td>
                                                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
                                                        {r.status === 'PENDING' && <button className="res-btn-confirm" onClick={() => handleConfirm(r.id)}>확정</button>}
                                                        {r.status !== 'CANCELLED' && <button className="res-btn-cancel" onClick={() => handleCancelRes(r.id)}>취소</button>}
                                                    </div>
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
