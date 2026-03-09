import { client } from '../../../api/client';

export interface Booking {
    id: number;
    title: string;
    officeId: number;
    roomId: number;
    roomName?: string; // Backend might need to join this or frontend fetches it
    officeName?: string;
    customerId: number;
    startAt: string; // ISO LocalDateTime
    endAt: string;   // ISO LocalDateTime
    status: 'PENDING_PAYMENT' | 'PENDING_APPROVAL' | 'CONFIRMED' | 'CANCELED';
    version?: number;
}

export interface CreateBookingRequest {
    title: string;
    officeId: number;
    roomId: number;
    customerId: number; // Required by Backend
    startAt: string;
    endAt: string;
}

interface ApiResponse<T> {
    status: string;
    message: string;
    data: T;
}

export const bookingApi = {
    // Get all bookings (optional filters can be added)
    getMyBookings: async (customerId?: number) => {
        // Backend doesn't support "me" endpoint, must filter by customerId
        // If customerId is unknown, this will fail or return empty
        const params = customerId ? { customerId } : {};
        const response = await client.get<ApiResponse<Booking[]>>('/reservations', { params });
        return response.data;
    },

    // Create a new booking
    createBooking: async (data: CreateBookingRequest) => {
        const response = await client.post<ApiResponse<Booking>>('/reservations', data);
        return response.data;
    },

    // Confirm booking
    confirmBooking: async (bookingId: number) => {
        const response = await client.patch<ApiResponse<Booking>>(`/reservations/${bookingId}/confirm`);
        return response.data;
    },

    // Cancel booking
    cancelBooking: async (bookingId: number) => {
        await client.post<ApiResponse<void>>(`/reservations/${bookingId}/cancel`, {});
    }
};
