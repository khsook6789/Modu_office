import { useState } from 'react';
import { reportApi, ISSUE_TYPE_OPTIONS, type ReportIssueType } from '../api/report.api';
import './FacilityReportModal.css';

interface Facility {
    id: number;
    facilityName: string;
}

interface Props {
    roomId: number;
    reservationId: number;
    facilities: Facility[];
    onClose: () => void;
    onSuccess: () => void;
}

export default function FacilityReportModal({ roomId, reservationId, facilities, onClose, onSuccess }: Props) {
    const [selectedFacilityId, setSelectedFacilityId] = useState<number | ''>('');
    const [selectedIssueType, setSelectedIssueType] = useState<ReportIssueType | null>(null);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const canSubmit = selectedFacilityId !== '' && selectedIssueType !== null && !submitting;

    const handleSubmit = async () => {
        if (!canSubmit) return;
        setSubmitting(true);
        setError(null);
        try {
            await reportApi.createReport(roomId, reservationId, Number(selectedFacilityId), selectedIssueType!);
            onSuccess();
            onClose();
        } catch (err: any) {
            const msg = err?.message || '신고 접수에 실패했습니다.';
            if (msg.includes('409') || msg.toLowerCase().includes('conflict')) {
                setError('동일한 시설에 이미 처리 중인 신고가 있습니다.');
            } else {
                setError(msg);
            }
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="report-modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
            <div className="report-modal">
                <div className="report-modal-header">
                    <span className="report-modal-title">🔧 시설 고장 신고</span>
                    <button className="report-modal-close" onClick={onClose}>×</button>
                </div>

                {/* 시설 선택 */}
                <label className="report-form-label">어떤 시설이 문제인가요?</label>
                {facilities.length === 0 ? (
                    <p style={{ color: '#64748b', fontSize: '0.875rem', marginBottom: '1.25rem' }}>
                        이 공간에 등록된 시설 정보가 없습니다.
                    </p>
                ) : (
                    <select
                        className="report-form-select"
                        value={selectedFacilityId}
                        onChange={(e) => setSelectedFacilityId(e.target.value === '' ? '' : Number(e.target.value))}
                    >
                        <option value="">시설 선택...</option>
                        {facilities.map((f) => (
                            <option key={f.id} value={f.id}>{f.facilityName}</option>
                        ))}
                    </select>
                )}

                {/* 이슈 유형 선택 */}
                <label className="report-form-label">문제 유형을 선택하세요</label>
                <div className="issue-type-grid">
                    {ISSUE_TYPE_OPTIONS.map((opt) => (
                        <button
                            key={opt.value}
                            type="button"
                            className={`issue-type-card ${selectedIssueType === opt.value ? 'selected' : ''}`}
                            onClick={() => setSelectedIssueType(opt.value)}
                        >
                            <span className="issue-type-icon">{opt.icon}</span>
                            <span className="issue-type-label">{opt.label}</span>
                        </button>
                    ))}
                </div>

                {/* 에러 메시지 */}
                {error && (
                    <p style={{ color: '#ef4444', fontSize: '0.82rem', marginBottom: '1rem', background: 'rgba(239,68,68,0.08)', borderRadius: '0.5rem', padding: '0.5rem 0.75rem' }}>
                        ⚠️ {error}
                    </p>
                )}

                <div className="report-modal-actions">
                    <button className="report-btn-cancel" onClick={onClose}>취소</button>
                    <button
                        className="report-btn-submit"
                        onClick={handleSubmit}
                        disabled={!canSubmit}
                    >
                        {submitting ? '접수 중...' : '신고 접수'}
                    </button>
                </div>
            </div>
        </div>
    );
}
