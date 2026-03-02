import { useState, useEffect } from 'react';
import { officeApi, type Office } from '../rooms/api/office.api';
import {
    statsApi,
    type OccupancyItem,
    type CancellationStats,
    type RoomRanking,
    type PeakTime,
    type DailyUsage,
} from './api/stats.api';
import './StatsDashboard.css';

export default function StatsDashboard() {
    const [offices, setOffices] = useState<Office[]>([]);
    const [selectedOfficeId, setSelectedOfficeId] = useState<number | undefined>();
    const [startDate, setStartDate] = useState(() => {
        const d = new Date();
        d.setDate(d.getDate() - 30);
        return d.toISOString().slice(0, 10);
    });
    const [endDate, setEndDate] = useState(() => new Date().toISOString().slice(0, 10));
    const [loading, setLoading] = useState(false);

    const [occupancy, setOccupancy] = useState<OccupancyItem[]>([]);
    const [cancellation, setCancellation] = useState<CancellationStats | null>(null);
    const [popularRooms, setPopularRooms] = useState<RoomRanking[]>([]);
    const [unpopularRooms, setUnpopularRooms] = useState<RoomRanking[]>([]);
    const [peakTimes, setPeakTimes] = useState<PeakTime[]>([]);
    const [dailyUsage, setDailyUsage] = useState<DailyUsage[]>([]);

    useEffect(() => {
        officeApi.getMyOffices().then(list => {
            setOffices(list);
            if (list.length > 0) setSelectedOfficeId(list[0].id);
        }).catch(() => {});
    }, []);

    useEffect(() => {
        loadStats();
    }, [selectedOfficeId, startDate, endDate]);

    const loadStats = async () => {
        setLoading(true);
        try {
            const [occ, cancel, popular, unpopular, peak, daily] = await Promise.all([
                statsApi.getOccupancy(selectedOfficeId),
                statsApi.getCancellationStats(selectedOfficeId, startDate, endDate),
                statsApi.getPopularRooms(selectedOfficeId, startDate, endDate),
                statsApi.getUnpopularRooms(selectedOfficeId, startDate, endDate),
                statsApi.getPeakTimes(selectedOfficeId, startDate, endDate),
                statsApi.getDailyUsage(selectedOfficeId, startDate, endDate),
            ]);
            setOccupancy(Array.isArray(occ) ? occ : []);
            setCancellation(cancel);
            setPopularRooms(Array.isArray(popular) ? popular : []);
            setUnpopularRooms(Array.isArray(unpopular) ? unpopular : []);
            setPeakTimes(Array.isArray(peak) ? peak : []);
            setDailyUsage(Array.isArray(daily) ? daily : []);
        } catch (e) {
            console.error('통계 로드 실패', e);
        } finally {
            setLoading(false);
        }
    };

    const maxPeak = Math.max(...peakTimes.map(p => p.count), 1);
    const maxDaily = Math.max(...dailyUsage.map(d => d.totalMinutes), 1);
    const occupiedCount = occupancy.filter(o => o.occupied).length;
    const occupancyRate = occupancy.length > 0 ? Math.round((occupiedCount / occupancy.length) * 100) : 0;

    return (
        <div className="stats-dashboard">
            {/* Filter Bar */}
            <div className="stats-filter-bar">
                <div className="stats-filter-group">
                    <label>오피스</label>
                    <select
                        value={selectedOfficeId ?? ''}
                        onChange={e => setSelectedOfficeId(e.target.value ? Number(e.target.value) : undefined)}
                        className="stats-select"
                    >
                        <option value="">전체</option>
                        {offices.map(o => (
                            <option key={o.id} value={o.id}>{o.name}</option>
                        ))}
                    </select>
                </div>
                <div className="stats-filter-group">
                    <label>시작일</label>
                    <input type="date" className="stats-input" value={startDate} onChange={e => setStartDate(e.target.value)} />
                </div>
                <div className="stats-filter-group">
                    <label>종료일</label>
                    <input type="date" className="stats-input" value={endDate} onChange={e => setEndDate(e.target.value)} />
                </div>
                <button className="stats-refresh-btn" onClick={loadStats} disabled={loading}>
                    {loading ? '로딩...' : '🔄 새로고침'}
                </button>
            </div>

            {loading ? (
                <div className="stats-loading">📊 통계를 불러오는 중...</div>
            ) : (
                <div className="stats-grid">
                    {/* 실시간 점유율 */}
                    <div className="stats-card stats-card-wide">
                        <h3 className="stats-card-title">⚡ 실시간 점유율</h3>
                        <div className="occupancy-summary">
                            <div className="occupancy-big-number">{occupancyRate}%</div>
                            <div className="occupancy-sub">{occupiedCount} / {occupancy.length} 회의실 사용 중</div>
                        </div>
                        <div className="occupancy-bar-bg">
                            <div className="occupancy-bar-fill" style={{ width: `${occupancyRate}%` }} />
                        </div>
                        <div className="occupancy-room-list">
                            {occupancy.length === 0 ? (
                                <p className="stats-empty">데이터가 없습니다.</p>
                            ) : occupancy.map(room => (
                                <div key={room.roomId} className={`occupancy-room-item ${room.occupied ? 'busy' : 'free'}`}>
                                    <span>{room.floor}F · {room.roomName}</span>
                                    <span className="occupancy-badge">{room.occupied ? '🔴 사용중' : '🟢 사용가능'}</span>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* 취소율 */}
                    <div className="stats-card">
                        <h3 className="stats-card-title">🚫 취소율 통계</h3>
                        {cancellation ? (
                            <div className="cancel-stats">
                                <div className="cancel-rate-circle">
                                    <span className="cancel-rate-num">{cancellation.cancellationRate?.toFixed(1) ?? 0}%</span>
                                    <span className="cancel-rate-label">취소율</span>
                                </div>
                                <div className="cancel-detail">
                                    <div className="cancel-row">
                                        <span>전체 예약</span>
                                        <strong>{cancellation.totalReservations}건</strong>
                                    </div>
                                    <div className="cancel-row">
                                        <span>취소 예약</span>
                                        <strong style={{ color: '#ef4444' }}>{cancellation.cancelledReservations}건</strong>
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <p className="stats-empty">데이터가 없습니다.</p>
                        )}
                    </div>

                    {/* 인기 회의실 */}
                    <div className="stats-card">
                        <h3 className="stats-card-title">🔥 인기 회의실 Top 5</h3>
                        {popularRooms.length === 0 ? (
                            <p className="stats-empty">데이터가 없습니다.</p>
                        ) : (
                            <div className="ranking-list">
                                {popularRooms.map((r, i) => (
                                    <div key={r.roomId} className="ranking-item">
                                        <span className={`ranking-badge rank-${i + 1}`}>{i + 1}</span>
                                        <span className="ranking-name">{r.roomName}</span>
                                        <span className="ranking-count">{r.reservationCount}건</span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* 비인기 회의실 */}
                    <div className="stats-card">
                        <h3 className="stats-card-title">💤 비인기 회의실 Top 5</h3>
                        {unpopularRooms.length === 0 ? (
                            <p className="stats-empty">데이터가 없습니다.</p>
                        ) : (
                            <div className="ranking-list">
                                {unpopularRooms.map((r, i) => (
                                    <div key={r.roomId} className="ranking-item unpopular">
                                        <span className="ranking-badge unpopular-badge">{i + 1}</span>
                                        <span className="ranking-name">{r.roomName}</span>
                                        <span className="ranking-count">{r.reservationCount}건</span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* 피크타임 분포 (바 차트) */}
                    <div className="stats-card stats-card-wide">
                        <h3 className="stats-card-title">⏰ 시간대별 예약 분포 (피크타임)</h3>
                        {peakTimes.length === 0 ? (
                            <p className="stats-empty">데이터가 없습니다.</p>
                        ) : (
                            <div className="bar-chart">
                                {Array.from({ length: 24 }, (_, hour) => {
                                    const found = peakTimes.find(p => p.hour === hour);
                                    const count = found?.count ?? 0;
                                    const height = maxPeak > 0 ? Math.round((count / maxPeak) * 100) : 0;
                                    return (
                                        <div key={hour} className="bar-col">
                                            <div className="bar-tooltip">{count}건</div>
                                            <div
                                                className={`bar-fill ${hour >= 9 && hour <= 18 ? 'peak' : 'off'}`}
                                                style={{ height: `${height}%` }}
                                            />
                                            <div className="bar-label">{hour}시</div>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>

                    {/* 일일 사용 시간 */}
                    <div className="stats-card stats-card-wide">
                        <h3 className="stats-card-title">📅 일별 회의실 총 사용 시간</h3>
                        {dailyUsage.length === 0 ? (
                            <p className="stats-empty">데이터가 없습니다.</p>
                        ) : (
                            <div className="daily-chart">
                                {dailyUsage.slice(-14).map(d => {
                                    const pct = Math.round((d.totalMinutes / maxDaily) * 100);
                                    const hrs = Math.floor(d.totalMinutes / 60);
                                    const mins = d.totalMinutes % 60;
                                    return (
                                        <div key={d.date} className="daily-row">
                                            <span className="daily-date">{d.date.slice(5)}</span>
                                            <div className="daily-bar-bg">
                                                <div className="daily-bar-fill" style={{ width: `${pct}%` }} />
                                            </div>
                                            <span className="daily-value">{hrs}h {mins}m</span>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
