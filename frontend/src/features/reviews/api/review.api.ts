import { client } from '../../../api/client';

export interface Review {
    id: number;
    roomId: number;
    userId: number;
    userName: string;
    rating: number; // 1-5
    comment: string;
    createdAt: string;
}

export interface CreateReviewRequest {
    reservationId: number;
    rating: number;
    content: string;  // 백엔드 필드명: content (comment 아님)
}

export const reviewApi = {
    // 특정 회의실 리뷰 목록 조회
    getReviewsByRoomId: async (roomId: number): Promise<Review[]> => {
        const response = await client.get<any>(`/reviews/room/${roomId}?size=100`);
        // 페이지 응답 또는 리스트 응답 모두 처리
        const raw = response.data;
        if (Array.isArray(raw)) return raw;
        if (raw?.content) return raw.content;
        return [];
    },

    // 리뷰 작성
    createReview: async (data: CreateReviewRequest): Promise<Review> => {
        const response = await client.post<Review>(`/reviews`, {
            reservationId: data.reservationId,
            rating: data.rating,
            content: data.content,
        });
        return response as any;
    },

    // 내 리뷰 목록
    getMyReviews: async (): Promise<Review[]> => {
        const response = await client.get<Review[]>('/reviews/me');
        return response as any;
    },

    // 리뷰 삭제
    deleteReview: async (reviewId: number): Promise<void> => {
        await client.delete(`/reviews/${reviewId}`);
    },
};
