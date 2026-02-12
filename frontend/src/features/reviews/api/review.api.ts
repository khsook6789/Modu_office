
export interface Review {
    id: string;
    roomId: number;
    userId: string;
    userName: string;
    rating: number; // 1-5
    comment: string;
    createdAt: string;
}

const REVIEW_STORAGE_KEY = 'reviews';

const getStoredReviews = (): Review[] => {
    const stored = localStorage.getItem(REVIEW_STORAGE_KEY);
    return stored ? JSON.parse(stored) : [];
};

export const reviewApi = {
    getReviewsByRoomId: (roomId: number): Promise<Review[]> => {
        return new Promise((resolve) => {
            setTimeout(() => {
                const reviews = getStoredReviews();
                const roomReviews = reviews.filter(r => r.roomId === roomId);
                // Sort by date desc
                roomReviews.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
                resolve(roomReviews);
            }, 300);
        });
    },

    createReview: (data: Omit<Review, 'id' | 'createdAt'>): Promise<Review> => {
        return new Promise((resolve) => {
            setTimeout(() => {
                const reviews = getStoredReviews();
                const newReview = {
                    ...data,
                    id: Date.now().toString(),
                    createdAt: new Date().toISOString()
                };
                localStorage.setItem(REVIEW_STORAGE_KEY, JSON.stringify([...reviews, newReview]));
                resolve(newReview);
            }, 500);
        });
    },

    deleteReview: (reviewId: string): Promise<void> => {
        return new Promise((resolve) => {
            setTimeout(() => {
                const reviews = getStoredReviews();
                const newReviews = reviews.filter(r => r.id !== reviewId);
                localStorage.setItem(REVIEW_STORAGE_KEY, JSON.stringify(newReviews));
                resolve();
            }, 300);
        });
    }
};
