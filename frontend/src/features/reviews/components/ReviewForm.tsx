import { useState, type FormEvent } from 'react';

interface ReviewFormProps {
    onSubmit: (rating: number, comment: string) => void;
    loading?: boolean;
}

export default function ReviewForm({ onSubmit, loading = false }: ReviewFormProps) {
    const [comment, setComment] = useState('');
    const [rating, setRating] = useState(5);
    const [hoverRating, setHoverRating] = useState(0);

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();
        onSubmit(rating, comment);
        setComment('');
        setRating(5);
    };

    return (
        <div className="bg-gray-50 p-md rounded-lg mb-lg">
            <h3 className="font-bold mb-sm text-sm">리뷰 작성하기</h3>
            <form onSubmit={handleSubmit}>
                <div className="flex items-center gap-2 mb-sm">
                    <span className="text-sm font-medium">별점:</span>
                    <div className="flex">
                        {[1, 2, 3, 4, 5].map((star) => (
                            <button
                                key={star}
                                type="button"
                                className="text-xl focus:outline-none transition-transform hover:scale-110"
                                style={{ 
                                    color: star <= (hoverRating || rating) ? '#fbbf24' : '#cbd5e1',
                                    background: 'none', border: 'none', cursor: 'pointer', padding: '0 2px'
                                }}
                                onClick={() => setRating(star)}
                                onMouseEnter={() => setHoverRating(star)}
                                onMouseLeave={() => setHoverRating(0)}
                            >
                                ★
                            </button>
                        ))}
                    </div>
                    <span className="text-sm text-muted ml-2 font-medium">{rating}점</span>
                </div>

                <div className="mb-sm">
                    <textarea
                        className="w-full p-sm border rounded text-sm focus:outline-none focus:border-primary"
                        style={{ resize: 'none' }}
                        rows={3}
                        placeholder="이용 경험을 공유해주세요 (선택사항)"
                        value={comment}
                        onChange={(e) => setComment(e.target.value)}
                        required
                    />
                </div>

                <div className="text-right">
                    <button
                        type="submit"
                        className="btn btn-primary btn-sm px-md"
                        disabled={loading}
                    >
                        {loading ? '등록 중...' : '리뷰 등록'}
                    </button>
                </div>
            </form>
        </div>
    );
}
