// import { client } from '../../../api/client';
// import { OfficeRoomResponse } from '../../rooms/api/room.api';

export interface Booking {
    id: number;
    roomId: number;
    roomName: string;
    officeName: string;
    date: string;
    startTime: string;
    endTime: string;
    guestCount: number;
    totalPrice: number;
    status: 'CONFIRMED' | 'CANCELLED' | 'PENDING';
    createdAt: string;
}

export interface CreateBookingRequest {
    roomId: number;
    date: string;
    startTime: string;
    endTime: string;
    guestCount: number;
    totalPrice: number;
}

// Mock Data Store (in memory for demo session)
let MOCK_BOOKINGS: Booking[] = [
    {
        id: 1,
        roomId: 101,
        roomName: 'Meeting Room A',
        officeName: '강남 공유오피스',
        date: '2023-10-25',
        startTime: '14:00',
        endTime: '16:00',
        guestCount: 4,
        totalPrice: 20000,
        status: 'CONFIRMED',
        createdAt: '2023-10-20T10:00:00'
    }
];

export const bookingApi = {
    // Get all bookings for the current user
    getMyBookings: () => {
        // return client.get<Booking[]>('/bookings/me');
        return Promise.resolve([...MOCK_BOOKINGS]);
    },

    // Create a new booking
    createBooking: (data: CreateBookingRequest) => {
        // return client.post<Booking>('/bookings', data);
        console.log('Mock Create Booking:', data);
        const newBooking: Booking = {
            id: Math.floor(Math.random() * 10000),
            ...data,
            roomName: 'Meeting Room (Mock)', // In real app, backend joins this
            officeName: 'Office (Mock)',     // In real app, backend joins this
            status: 'PENDING', // Initially pending until payment
            createdAt: new Date().toISOString()
        };
        // We don't push to MOCK_BOOKINGS here because strictly we should do it after payment success
        // But for "createBooking" step usually returns an ID to proceed to payment
        return Promise.resolve(newBooking);
    },

    // Confirm booking (after payment)
    confirmBooking: (bookingId: number) => {
        // return client.post(`/bookings/${bookingId}/confirm`);
        // Find if it exists in a "pending" list or just mock it
        const booking = {
            id: bookingId,
            roomId: 101,
            roomName: 'Meeting Room (Mock)',
            officeName: 'Office (Mock)',
            date: new Date().toISOString().split('T')[0],
            startTime: '10:00',
            endTime: '12:00',
            guestCount: 3,
            totalPrice: 30000,
            status: 'CONFIRMED' as const,
            createdAt: new Date().toISOString()
        };
        MOCK_BOOKINGS.unshift(booking);
        return Promise.resolve(booking);
    },

    // Cancel booking
    cancelBooking: (bookingId: number) => {
        // return client.post(`/bookings/${bookingId}/cancel`);
        MOCK_BOOKINGS = MOCK_BOOKINGS.map(b =>
            b.id === bookingId ? { ...b, status: 'CANCELLED' } : b
        );
        return Promise.resolve();
    }
};
