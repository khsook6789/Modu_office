import { useState } from 'react';
import type { Review } from '../api/review.api';
import { reviewApi } from '../api/review.api';
import { useAuth } from '../../../contexts/AuthContext';

interface ReviewListProps {
    reviews: Review[];
    onDelete?: (id: number) => void;
    onUpdate?: (updated: Review) => void;
}

export default function ReviewList({ reviews, onDelete, onUpdate }: ReviewListProps) {
    const { user } = useAuth();
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editRating, setEditRating] = useState(5);
    const [editComment, setEditComment] = useState('');
    const [editLoading, setEditLoading] = useState(false);

    const startEdit = (review: Review) => {
        setEditingId(review.id);
        setEditRating(review.rating);
        setEditComment(review.comment);
    };

    const cancelEdit = () => setEditingId(null);

    const submitEdit = async (reviewId: number) => {
        setEditLoading(true);
        try {
            const updated = await reviewApi.updateReview(reviewId, { rating: editRating, content: editComment });
            onUpdate?.(updated);
            setEditingId(null);
        } catch {
        } finally {
            setEditLoading(false);
        }
    };

    if (reviews.length === 0) {
        return (
            <div className="text-center py-lg text-muted bg-gray-50 rounded-lg">
                <p>아직 리뷰가 없습니다. 첫 번째 리뷰를 남겨보세요!</p>
            </div>
        );
    }

    const isOwner = (review: Review) =>
        user && (Number(user.id) === review.userId || user.role === 'ADMIN');

    return (
        <div className="space-y-md">
            {reviews.map((review) => (
                <div key={review.id} className="border-b pb-md last:border-0">
                    <div className="flex justify-between items-start mb-xs">
                        <div>
                            <span className="font-bold mr-sm">{review.userName}</span>
                            <span className="text-yellow-400 text-sm">{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}</span>
                        </div>
                        <span className="text-xs text-muted">
                            {new Date(review.createdAt).toLocaleDateString('ko-KR')}
                        </span>
                    </div>

                    {editingId === review.id ? (
                        <div style={{ marginTop: '0.5rem' }}>
                            {/* 별점 선택 */}
                            <div style={{ display: 'flex', gap: '4px', marginBottom: '0.5rem' }}>
                                {[1, 2, 3, 4, 5].map(s => (
                                    <button key={s} type="button" onClick={() => setEditRating(s)}
                                        style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '1.25rem', color: s <= editRating ? '#f59e0b' : '#d1d5db' }}>
                                        ★
                                    </button>
                                ))}
                            </div>
                            <textarea
                                value={editComment}
                                onChange={e => setEditComment(e.target.value)}
                                style={{ width: '100%', padding: '0.5rem 0.75rem', borderRadius: 'var(--radius-md)', border: '1px solid #e2e8f0', fontSize: '0.875rem', resize: 'vertical', minHeight: '72px', outline: 'none' }}
                            />
                            <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
                                <button onClick={() => submitEdit(review.id)} disabled={editLoading}
                                    className="btn btn-primary" style={{ padding: '0.4rem 1rem', fontSize: '0.8rem' }}>
                                    {editLoading ? '저장 중...' : '저장'}
                                </button>
                                <button onClick={cancelEdit}
                                    className="btn btn-secondary" style={{ padding: '0.4rem 1rem', fontSize: '0.8rem' }}>
                                    취소
                                </button>
                            </div>
                        </div>
                    ) : (
                        <>
                            <p className="text-gray-700 text-sm mb-xs">{review.comment}</p>
                            {isOwner(review) && (
                                <div style={{ display: 'flex', gap: '0.75rem' }}>
                                    <button onClick={() => startEdit(review)}
                                        className="text-xs" style={{ color: 'var(--color-primary)', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}>
                                        수정
                                    </button>
                                    {onDelete && (
                                        <button onClick={() => onDelete(review.id)}
                                            className="text-xs" style={{ color: '#ef4444', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}>
                                            삭제
                                        </button>
                                    )}
                                </div>
                            )}
                        </>
                    )}
                </div>
            ))}
        </div>
    );
}
