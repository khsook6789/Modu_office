import type { Review } from '../api/review.api';
import { useAuth } from '../../../contexts/AuthContext';

interface ReviewListProps {
    reviews: Review[];
    onDelete?: (id: string) => void;
}

export default function ReviewList({ reviews, onDelete }: ReviewListProps) {
    const { user } = useAuth();

    if (reviews.length === 0) {
        return (
            <div className="text-center py-lg text-muted bg-gray-50 rounded-lg">
                <p>아직 리뷰가 없습니다. 첫 번째 리뷰를 남겨보세요!</p>
            </div>
        );
    }

    return (
        <div className="space-y-md">
            {reviews.map((review) => (
                <div key={review.id} className="border-b pb-md last:border-0">
                    <div className="flex justify-between items-start mb-xs">
                        <div>
                            <span className="font-bold mr-sm">{review.userName}</span>
                        </div>
                        <span className="text-xs text-muted">
                            {new Date(review.createdAt).toLocaleDateString()}
                        </span>
                    </div>
                    <p className="text-gray-700 text-sm mb-xs">{review.comment}</p>

                    {/* Allow deletion if user is author or admin */}
                    {onDelete && user && (user.id === review.userId || user.role === 'PLATFORM_ADMIN') && (
                        <button
                            onClick={() => onDelete(review.id)}
                            className="text-xs text-red-500 hover:text-red-700 underline"
                        >
                            삭제
                        </button>
                    )}
                </div>
            ))}
        </div>
    );
}
