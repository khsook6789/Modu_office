
export interface WishlistItem {
    userId: string;
    roomId: number;
    createdAt: string;
}

const WISHLIST_STORAGE_KEY = 'wishlist';

const getStoredWishlist = (): WishlistItem[] => {
    const stored = localStorage.getItem(WISHLIST_STORAGE_KEY);
    return stored ? JSON.parse(stored) : [];
};

export const wishlistApi = {
    getWishlist: (userId: string): Promise<number[]> => { // Returns room IDs
        return new Promise((resolve) => {
            setTimeout(() => {
                const wishlist = getStoredWishlist();
                const userItems = wishlist.filter(item => item.userId === userId);
                resolve(userItems.map(item => item.roomId));
            }, 200);
        });
    },

    toggleWishlist: (userId: string, roomId: number): Promise<boolean> => { // Returns new state (isLiked)
        return new Promise((resolve) => {
            setTimeout(() => {
                const wishlist = getStoredWishlist();
                const existingIndex = wishlist.findIndex(item => item.userId === userId && item.roomId === roomId);

                let newWishlist;
                let isLiked;

                if (existingIndex > -1) {
                    // Remove
                    newWishlist = wishlist.filter((_, index) => index !== existingIndex);
                    isLiked = false;
                } else {
                    // Add
                    newWishlist = [...wishlist, { userId, roomId, createdAt: new Date().toISOString() }];
                    isLiked = true;
                }

                localStorage.setItem(WISHLIST_STORAGE_KEY, JSON.stringify(newWishlist));
                resolve(isLiked);
            }, 200);
        });
    },

    // Check if a specific room is liked (helper for single room checking if not passing full list)
    isLiked: (userId: string, roomId: number): Promise<boolean> => {
        return new Promise((resolve) => {
            const wishlist = getStoredWishlist();
            const exists = wishlist.some(item => item.userId === userId && item.roomId === roomId);
            resolve(exists);
        });
    }
};
