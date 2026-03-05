import { client } from '../../../api/client';

export interface PaymentConfirmRequest {
    paymentKey: string;
    orderId: string;
    amount: number;
    reservationId: number;
}

export interface PaymentResponse {
    id: number;
    reservationId: number;
    orderId: string;
    paymentKey: string;
    amount: number;
    status: 'READY' | 'IN_PROGRESS' | 'DONE' | 'CANCELED' | 'PARTIAL_CANCELED' | 'FAILED';
    method: string | null;
    approvedAt: string | null;
    canceledAt: string | null;
}

interface ApiResponse<T> {
    status: string;
    message: string;
    data: T;
}

export const paymentApi = {
    confirm: async (request: PaymentConfirmRequest): Promise<PaymentResponse> => {
        // client.post<T>는 Promise<T>를 반환 (커스텀 ApiClient — fetch 기반)
        const res = await client.post<ApiResponse<PaymentResponse>>('/payments/confirm', request);
        return res.data;
    },

    getByReservation: async (reservationId: number): Promise<PaymentResponse> => {
        const res = await client.get<ApiResponse<PaymentResponse>>(`/payments/${reservationId}`);
        return res.data;
    },
};
