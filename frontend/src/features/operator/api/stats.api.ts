import { client } from '../../../api/client';

export interface OccupancyItem {
    roomId: number;
    roomName: string;
    floor: number;
    occupied: boolean;
    currentReservationId?: number;
}

export interface CancellationStats {
    totalReservations: number;
    cancelledReservations: number;
    cancellationRate: number;
}

export interface RoomRanking {
    roomId: number;
    roomName: string;
    reservationCount: number;
}

export interface PeakTime {
    hour: number;
    count: number;
}

export interface DailyUsage {
    date: string;
    totalMinutes: number;
}

export const statsApi = {
    /** 실시간 점유율 */
    getOccupancy: async (officeId?: number, floor?: number): Promise<OccupancyItem[]> => {
        const params: Record<string, any> = {};
        if (officeId) params.officeId = officeId;
        if (floor) params.floor = floor;
        const res = await client.get<any>('/admin/stats/occupancy', { params });
        const raw = res.data ?? res;
        return raw?.data ?? raw ?? [];
    },

    /** 취소율 통계 */
    getCancellationStats: async (officeId?: number, startDate?: string, endDate?: string): Promise<CancellationStats> => {
        const params: Record<string, any> = {};
        if (officeId) params.officeId = officeId;
        if (startDate) params.startDate = startDate;
        if (endDate) params.endDate = endDate;
        const res = await client.get<any>('/admin/stats/cancellations', { params });
        const raw = res.data ?? res;
        return raw?.data ?? raw ?? { totalReservations: 0, cancelledReservations: 0, cancellationRate: 0 };
    },

    /** 인기 회의실 Top5 */
    getPopularRooms: async (officeId?: number, startDate?: string, endDate?: string): Promise<RoomRanking[]> => {
        const params: Record<string, any> = {};
        if (officeId) params.officeId = officeId;
        if (startDate) params.startDate = startDate;
        if (endDate) params.endDate = endDate;
        const res = await client.get<any>('/admin/stats/rooms/popular', { params });
        const raw = res.data ?? res;
        return raw?.data ?? raw ?? [];
    },

    /** 비인기 회의실 Top5 */
    getUnpopularRooms: async (officeId?: number, startDate?: string, endDate?: string): Promise<RoomRanking[]> => {
        const params: Record<string, any> = {};
        if (officeId) params.officeId = officeId;
        if (startDate) params.startDate = startDate;
        if (endDate) params.endDate = endDate;
        const res = await client.get<any>('/admin/stats/rooms/unpopular', { params });
        const raw = res.data ?? res;
        return raw?.data ?? raw ?? [];
    },

    /** 피크타임 분포 */
    getPeakTimes: async (officeId?: number, startDate?: string, endDate?: string): Promise<PeakTime[]> => {
        const params: Record<string, any> = {};
        if (officeId) params.officeId = officeId;
        if (startDate) params.startDate = startDate;
        if (endDate) params.endDate = endDate;
        const res = await client.get<any>('/admin/stats/peak-times', { params });
        const raw = res.data ?? res;
        return raw?.data ?? raw ?? [];
    },

    /** 일일 총 사용 시간 */
    getDailyUsage: async (officeId?: number, startDate?: string, endDate?: string): Promise<DailyUsage[]> => {
        const params: Record<string, any> = {};
        if (officeId) params.officeId = officeId;
        if (startDate) params.startDate = startDate;
        if (endDate) params.endDate = endDate;
        const res = await client.get<any>('/admin/stats/daily-usage', { params });
        const raw = res.data ?? res;
        return raw?.data ?? raw ?? [];
    },
};
