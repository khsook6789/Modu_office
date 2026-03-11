import { client } from '../../../api/client';

export type ReportIssueType =
    | 'BROKEN'
    | 'MALFUNCTION'
    | 'NEEDS_SUPPLIES'
    | 'DIRTY'
    | 'MISSING'
    | 'OTHER';

export type ReportStatus = 'REPORTED' | 'IN_PROGRESS' | 'RESOLVED' | 'CANCELED';

export interface FacilityReport {
    reportId: number;
    reservationId: number;
    facilityId: number;
    facilityName: string;
    issueType: ReportIssueType;
    issueTypeName: string;
    status: ReportStatus;
    statusName: string;
    createdAt: string;
}

export const ISSUE_TYPE_OPTIONS: { value: ReportIssueType; label: string; icon: string }[] = [
    { value: 'BROKEN',        label: '고장',         icon: '💥' },
    { value: 'MALFUNCTION',   label: '오작동',        icon: '⚠️' },
    { value: 'NEEDS_SUPPLIES',label: '소모품 부족',   icon: '📦' },
    { value: 'DIRTY',         label: '청결 불량',     icon: '🧹' },
    { value: 'MISSING',       label: '비품 없음',     icon: '❌' },
    { value: 'OTHER',         label: '기타',          icon: '📝' },
];

export const reportApi = {
    createReport: async (
        roomId: number,
        reservationId: number,
        facilityId: number,
        issueType: ReportIssueType,
    ): Promise<FacilityReport> => {
        const res = await client.post<any>(`/rooms/${roomId}/reports`, {
            reservationId,
            facilityId,
            issueType,
        });
        // 응답이 직접 FacilityReportResponse이거나 data 안에 있을 수 있음
        return (res?.data ?? res) as FacilityReport;
    },

    getMyReports: async (reservationId: number): Promise<FacilityReport[]> => {
        const res = await client.get<any>(`/my-reports?reservationId=${reservationId}`);
        const raw = res?.data ?? res;
        // 배열이면 그대로, 아니면 data 안을 확인
        if (Array.isArray(raw)) return raw;
        if (Array.isArray(raw?.data)) return raw.data;
        return [];
    },

    cancelReport: async (reportId: number): Promise<FacilityReport> => {
        const res = await client.patch<any>(`/reports/${reportId}/cancel`);
        return (res?.data ?? res) as FacilityReport;
    },
};
