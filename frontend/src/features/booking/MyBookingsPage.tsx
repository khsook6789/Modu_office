import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { client } from '../../api/client';
import { useAuth } from '../../contexts/AuthContext';
import { reportApi, type FacilityReport } from '../reports/api/report.api';
import FacilityReportModal from '../reports/components/FacilityReportModal';
import './MyBookings.css';
import '../reports/components/FacilityReportModal.css';

interface ReservationResponse {
    id: number;
    title: string;
    officeId: number;
    officeName: string;
    roomId: number;
    roomName: string;
    roomCode: string;
    userId: number;
    userName: string;
    startAt: string;
    endAt: string;
    status: 'PENDING_PAYMENT' | 'PENDING_APPROVAL' | 'CONFIRMED' | 'CANCELED';
    totalPrice: number | null;
    createdAt: string;
}

interface ApiResponse<T> {
    status: string;
    message: string;
    data: T;
}

interface ReportModalTarget {
    roomId: number;
    reservationId: number;
    facilities: { id: number; facilityName: string }[];
}

export default function MyBookingsPage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [bookings, setBookings] = useState<ReservationResponse[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [filter, setFilter] = useState<'ALL' | 'PENDING_PAYMENT' | 'PENDING_APPROVAL' | 'CONFIRMED' | 'CANCELED'>('ALL');

    // 환불 예상액 모달
    const [refundModal, setRefundModal] = useState<{
        bookingId: number;
        totalPrice: number;
        refundAmount: number;
        cancellationPenalty: number;
        refundRate: number;
        reasonDescriptor: string;
    } | null>(null);
    const [cancellingId, setCancellingId] = useState<number | null>(null);

    // 신고 관련 상태
    const [reportModalTarget, setReportModalTarget] = useState<ReportModalTarget | null>(null);
    const [reportsMap, setReportsMap] = useState<Record<number, FacilityReport[]>>({});
    const [expandedReports, setExpandedReports] = useState<Set<number>>(new Set());

    // PENDING_PAYMENT 카운트다운 (초 단위 남은 시간)
    const [countdowns, setCountdowns] = useState<Record<number, number>>({});
    const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

    // 10분(600초) 카운트다운 계산
    const calcCountdowns = useCallback((list: ReservationResponse[]) => {
        const now = Date.now();
        const map: Record<number, number> = {};
        list.forEach(b => {
            if (b.status === 'PENDING_PAYMENT') {
                const expiry = new Date(b.createdAt).getTime() + 10 * 60 * 1000;
                map[b.id] = Math.max(0, Math.floor((expiry - now) / 1000));
            }
        });
        return map;
    }, []);

    const loadBookings = useCallback(async () => {
        try {
            setIsLoading(true);
            setError(null);
            const response = await client.get<ApiResponse<ReservationResponse[]>>(
                `/reservations?userId=${user!.id}`
            );
            // client는 raw JSON({ status, message, data: [...] })을 반환
            const list: ReservationResponse[] = Array.isArray(response.data) ? response.data : [];
            const sorted = [...list].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
            setBookings(sorted);

            // 카운트다운 초기화 + 인터밸 시작
            if (timerRef.current) clearInterval(timerRef.current);
            const hasPending = sorted.some(b => b.status === 'PENDING_PAYMENT');
            if (hasPending) {
                setCountdowns(calcCountdowns(sorted));
                // stale closure 방지: setInterval 내에서 현재 bookings state를 직접 참조
                timerRef.current = setInterval(() => {
                    setBookings(prev => {
                        setCountdowns(calcCountdowns(prev));
                        return prev;
                    });
                }, 1000);
            } else {
                setCountdowns({});
            }
        } catch (err: any) {
            console.error('Failed to load bookings:', err);
            setError('예약 목록을 불러오는 데 실패했습니다. (데이터가 없거나 서버 오류)');
            setBookings([]);
        } finally {
            setIsLoading(false);
        }
    }, [user, calcCountdowns]);

    useEffect(() => {
        if (user?.id) loadBookings();
    }, [user, loadBookings]);


    // 언마운트 시 인터밸 정리
    useEffect(() => {
        return () => { if (timerRef.current) clearInterval(timerRef.current); };
    }, []);


    // 신고 내역 조회
    const loadReports = async (reservationId: number) => {
        try {
            const reports = await reportApi.getMyReports(reservationId);
            setReportsMap(prev => ({ ...prev, [reservationId]: reports }));
        } catch {
            setReportsMap(prev => ({ ...prev, [reservationId]: [] }));
        }
    };

    // 신고 내역 펼치기/접기
    const toggleReports = (reservationId: number) => {
        setExpandedReports(prev => {
            const next = new Set(prev);
            if (next.has(reservationId)) {
                next.delete(reservationId);
            } else {
                next.add(reservationId);
                if (!reportsMap[reservationId]) {
                    loadReports(reservationId);
                }
            }
            return next;
        });
    };

    // 신고 철회
    const handleWithdrawReport = async (reportId: number, reservationId: number) => {
        try {
            await reportApi.cancelReport(reportId);
            await loadReports(reservationId);
        } catch (err: any) {
            alert('신고 철회 실패: ' + (err?.message || '서버 오류'));
        }
    };

    // 방의 시설 목록 조회 후 신고 모달 열기
    const handleOpenReportModal = async (booking: ReservationResponse) => {
        try {
            const res = await client.get<any>(`/rooms/${booking.roomId}`);
            const raw = res?.data ?? res;
            const roomData = raw?.data ?? raw;
            const facilities: { id: number; facilityName: string }[] =
                (roomData?.facilities ?? []).map((f: any) => ({ id: f.id, facilityName: f.facilityName }));
            setReportModalTarget({ roomId: booking.roomId, reservationId: booking.id, facilities });
        } catch {
            setReportModalTarget({ roomId: booking.roomId, reservationId: booking.id, facilities: [] });
        }
    };

    const handleCancel = async (bookingId: number) => {
        try {
            const res = await client.get<any>(`/reservations/${bookingId}/refund-preview`);
            const raw = res?.data ?? res;
            const preview = raw?.data ?? raw;
            setRefundModal({
                bookingId,
                totalPrice: Number(preview.totalPrice ?? 0),
                refundAmount: Number(preview.refundAmount ?? 0),
                cancellationPenalty: Number(preview.cancellationPenalty ?? 0),
                refundRate: preview.refundRate ?? 100,
                reasonDescriptor: preview.reasonDescriptor ?? '',
            });
        } catch {
            if (window.confirm('정말로 이 예약을 취소하시겠습니까?')) {
                await doCancel(bookingId);
            }
        }
    };

    const doCancel = async (bookingId: number) => {
        setCancellingId(bookingId);
        try {
            await client.post(`/reservations/${bookingId}/cancel`, null);
            setRefundModal(null);
            await loadBookings();
        } catch (err: any) {
            alert('예약 취소 실패: ' + (err?.message || '서버 오류'));
        } finally {
            setCancellingId(null);
        }
    };

    const today = new Date();

    const getStatusInfo = (booking: ReservationResponse) => {
        if (booking.status === 'CANCELED') return { label: '취소됨', cls: 'CANCELED' };
        if (new Date(booking.endAt) < today) return { label: '이용 완료', cls: 'DONE' };
        if (booking.status === 'PENDING_PAYMENT') return { label: '결제 대기', cls: 'PENDING_PAYMENT' };
        if (booking.status === 'PENDING_APPROVAL') return { label: '승인 대기', cls: 'PENDING_APPROVAL' };
        return { label: '예약 확정', cls: 'CONFIRMED' };
    };

    const filtered = filter === 'ALL'
        ? bookings
        : bookings.filter(b => b.status === filter);

    const stats = {
        total: bookings.length,
        pendingPayment: bookings.filter(b => b.status === 'PENDING_PAYMENT').length,
        pendingApproval: bookings.filter(b => b.status === 'PENDING_APPROVAL').length,
        confirmed: bookings.filter(b => b.status === 'CONFIRMED').length,
        canceled: bookings.filter(b => b.status === 'CANCELED').length,
    };

    if (isLoading) return <div className="my-bookings-page"><div className="booking-empty"><p>로딩 중...</p></div></div>;

    return (
        <div className="my-bookings-page">
            {/* 환불 예상액 모달 */}
            {refundModal && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <div style={{ background: '#1e293b', borderRadius: '1.25rem', padding: '2rem', width: '360px', border: '1px solid rgba(255,255,255,0.1)' }}>
                        <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1.25rem', color: '#f1f5f9' }}>🔍 취소 전 환불 예상액</h3>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem', fontSize: '0.9rem', marginBottom: '1rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ color: '#94a3b8' }}>결제 금액</span>
                                <span style={{ color: '#f1f5f9' }}>{refundModal.totalPrice.toLocaleString()}원</span>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ color: '#94a3b8' }}>위약금</span>
                                <span style={{ color: '#ef4444' }}>-{refundModal.cancellationPenalty.toLocaleString()}원</span>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 700, borderTop: '1px solid rgba(255,255,255,0.1)', paddingTop: '0.6rem' }}>
                                <span style={{ color: '#94a3b8' }}>환불 예상액</span>
                                <span style={{ color: '#34d399', fontSize: '1.05rem' }}>{refundModal.refundAmount.toLocaleString()}원</span>
                            </div>
                        </div>
                        {refundModal.reasonDescriptor && (
                            <p style={{ fontSize: '0.78rem', color: '#64748b', background: 'rgba(255,255,255,0.04)', borderRadius: '0.5rem', padding: '0.5rem 0.75rem', marginBottom: '1.25rem' }}>
                                ℹ️ {refundModal.reasonDescriptor}
                            </p>
                        )}
                        <div style={{ display: 'flex', gap: '0.75rem' }}>
                            <button
                                onClick={() => setRefundModal(null)}
                                style={{ flex: 1, padding: '0.75rem', background: 'transparent', border: '1px solid #475569', borderRadius: '0.75rem', color: '#94a3b8', cursor: 'pointer' }}
                            >닫기</button>
                            <button
                                onClick={() => doCancel(refundModal.bookingId)}
                                disabled={cancellingId !== null}
                                style={{ flex: 1, padding: '0.75rem', background: '#ef4444', border: 'none', borderRadius: '0.75rem', color: '#fff', fontWeight: 600, cursor: 'pointer' }}
                            >{cancellingId ? '취소 중...' : '예약 취소 확정'}</button>
                        </div>
                    </div>
                </div>
            )}

            {/* Header */}
            <div className="bookings-header">
                <h1 className="bookings-title">내 <span>예약</span> 내역</h1>
                <button className="bookings-new-btn" onClick={() => navigate('/rooms')}>+ 새 예약하기</button>
            </div>

            {/* Error Banner */}
            {error && (
                <div style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '0.875rem', padding: '0.875rem 1.25rem', marginBottom: '1.25rem', color: '#dc2626', fontSize: '0.875rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>⚠️ {error}</span>
                    <button onClick={loadBookings} style={{ background: '#dc2626', color: '#fff', border: 'none', borderRadius: '0.5rem', padding: '0.3rem 0.75rem', fontSize: '0.8rem', cursor: 'pointer' }}>다시 시도</button>
                </div>
            )}

            {/* Stats */}
            <div className="bookings-stats">
                <div className="book-stat-card">
                    <span className="book-stat-icon">📋</span>
                    <div><div className="book-stat-value">{stats.total}</div><div className="book-stat-label">전체</div></div>
                </div>
                <div className="book-stat-card">
                    <span className="book-stat-icon" style={{ background: '#fffbeb' }}>💳</span>
                    <div><div className="book-stat-value" style={{ color: '#b45309' }}>{stats.pendingPayment}</div><div className="book-stat-label">결제 대기</div></div>
                </div>
                <div className="book-stat-card">
                    <span className="book-stat-icon" style={{ background: '#fef08a' }}>⏳</span>
                    <div><div className="book-stat-value" style={{ color: '#854d0e' }}>{stats.pendingApproval}</div><div className="book-stat-label">승인 대기</div></div>
                </div>
                <div className="book-stat-card">
                    <span className="book-stat-icon" style={{ background: '#f0fdf4' }}>✅</span>
                    <div><div className="book-stat-value" style={{ color: '#15803d' }}>{stats.confirmed}</div><div className="book-stat-label">확정</div></div>
                </div>
                <div className="book-stat-card">
                    <span className="book-stat-icon" style={{ background: '#fef2f2' }}>❌</span>
                    <div><div className="book-stat-value" style={{ color: '#dc2626' }}>{stats.canceled}</div><div className="book-stat-label">취소</div></div>
                </div>
            </div>

            {/* Filter tabs */}
            <div className="book-filter-tabs">
                {[['ALL', '전체'], ['PENDING_PAYMENT', '결제 대기'], ['PENDING_APPROVAL', '승인 대기'], ['CONFIRMED', '확정'], ['CANCELED', '취소']].map(([v, l]) => (
                    <button
                        key={v}
                        className={'book-filter-btn' + (filter === v ? ' active' : '')}
                        onClick={() => setFilter(v as any)}
                    >{l}</button>
                ))}
            </div>

            {filtered.length === 0 ? (
                <div className="booking-empty">
                    <p style={{ fontSize: '2.5rem', marginBottom: '0.75rem' }}>📭</p>
                    <p style={{ fontWeight: 600, fontSize: '1rem', color: '#334155' }}>예약 내역이 없습니다</p>
                    <p style={{ fontSize: '0.875rem', marginTop: '0.25rem' }}>회의실을 예약하면 여기에 표시됩니다.</p>
                </div>
            ) : (
                <div className="bookings-list">
                    {filtered.map(booking => {
                        const { label, cls } = getStatusInfo(booking);
                        const isPast = new Date(booking.endAt) < today;
                        const canCancel = booking.status !== 'CANCELED' && !isPast;

                        // 신고 버튼: CONFIRMED + 예약 시작 이후
                        const hasStarted = new Date(booking.startAt) <= today;
                        const canReport = booking.status === 'CONFIRMED' && hasStarted;
                        const isReportsExpanded = expandedReports.has(booking.id);
                        const bookingReports = reportsMap[booking.id] ?? [];
                        const activeReportCount = bookingReports.filter(r => r.status !== 'CANCELED').length;

                        // 결제 카운트다운
                        const secsLeft = countdowns[booking.id];
                        const isExpired = secsLeft === 0;
                        const isUrgent = secsLeft !== undefined && secsLeft <= 180;
                        const timerLabel = secsLeft !== undefined
                            ? (isExpired ? '⏰ 만료됨 (잠시 후 자동 취소)' : ('⏱ 결제 마감: ' + String(Math.floor(secsLeft / 60)).padStart(2, '0') + ':' + String(secsLeft % 60).padStart(2, '0')))
                            : null;

                        return (
                            <div key={booking.id} className={'booking-card' + (cls === 'CANCELED' || cls === 'DONE' ? ' muted' : '')}>
                                <div className="booking-card-left">
                                    <div className="booking-card-top">
                                        <span className={'booking-badge ' + cls}>{label}</span>
                                        <span className="booking-date-created">{booking.createdAt.substring(0, 10)} 예약</span>
                                    </div>
                                    <h3 className="booking-room-name">
                                        {booking.roomName}
                                        <span className="booking-room-code">({booking.roomCode})</span>
                                    </h3>
                                    {booking.title && <p className="booking-title-text">📝 {booking.title}</p>}
                                    <div className="booking-meta">
                                        <span>🏢 {booking.officeName}</span>
                                        <span>🕐 {new Date(booking.startAt).toLocaleString('ko-KR', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })} ~ {new Date(booking.endAt).toLocaleString('ko-KR', { hour: '2-digit', minute: '2-digit' })}</span>
                                    </div>

                                    {/* 결제 카운트다운 배너 */}
                                    {booking.status === 'PENDING_PAYMENT' && timerLabel && (
                                        <div className={'payment-timer' + (isExpired ? ' expired' : isUrgent ? ' urgent' : '')}>
                                            {timerLabel}
                                        </div>
                                    )}
                                    {/* 신고 내역 인라인 */}
                                    {isReportsExpanded && (
                                        <div className="report-list">
                                            {bookingReports.length === 0 ? (
                                                <p style={{ color: '#64748b', fontSize: '0.8rem', padding: '0.35rem 0' }}>신고 내역이 없습니다.</p>
                                            ) : bookingReports.map(report => (
                                                <div key={report.reportId} className="report-item">
                                                    <div className="report-item-info">
                                                        <span className="report-item-name">{report.facilityName}</span>
                                                        <span className="report-item-issue"> · {report.issueTypeName}</span>
                                                    </div>
                                                    <span className={'report-status-badge ' + report.status}>{report.statusName}</span>
                                                    {report.status === 'REPORTED' && (
                                                        <button
                                                            className="report-withdraw-btn"
                                                            onClick={() => handleWithdrawReport(report.reportId, booking.id)}
                                                        >철회</button>
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>

                                <div className="booking-card-right">
                                    <div className="booking-price">
                                        {booking.totalPrice != null
                                            ? <span>{Number(booking.totalPrice).toLocaleString()}원</span>
                                            : <span className="price-unknown">가격 미정</span>
                                        }
                                    </div>
                                    {canReport && (
                                        <button
                                            className="booking-cancel-btn"
                                            style={{ background: 'linear-gradient(135deg,#0ea5e9,#6366f1)', borderColor: 'transparent', color: '#fff', marginBottom: '0.5rem' }}
                                            onClick={() => handleOpenReportModal(booking)}
                                        >
                                            🔧 고장 신고
                                        </button>
                                    )}
                                    {canReport && (
                                        <button
                                            className="booking-cancel-btn"
                                            style={{ background: 'transparent', fontSize: '0.78rem', padding: '0.4rem 0.875rem', marginBottom: '0.5rem' }}
                                            onClick={() => toggleReports(booking.id)}
                                        >
                                            {isReportsExpanded ? '▲ 신고 내역 닫기' : ('📋 신고 내역' + (activeReportCount > 0 ? ' (' + activeReportCount + ')' : ''))}
                                        </button>
                                    )}
                                    {canCancel && (
                                        <button className="booking-cancel-btn" onClick={() => handleCancel(booking.id)}>
                                            예약 취소
                                        </button>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}

            {/* 신고 모달 */}
            {reportModalTarget && (
                <FacilityReportModal
                    roomId={reportModalTarget.roomId}
                    reservationId={reportModalTarget.reservationId}
                    facilities={reportModalTarget.facilities}
                    onClose={() => setReportModalTarget(null)}
                    onSuccess={() => {
                        if (reportModalTarget) {
                            loadReports(reportModalTarget.reservationId);
                            setExpandedReports(prev => {
                                const next = new Set(prev);
                                next.add(reportModalTarget.reservationId);
                                return next;
                            });
                        }
                    }}
                />
            )}
        </div>
    );
}
