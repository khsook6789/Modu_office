import { useState, type FormEvent } from 'react';

interface ReviewFormProps {
    onSubmit: (rating: number, comment: string) => void;
    loading?: boolean;
}

export default function ReviewForm({ onSubmit, loading = false }: ReviewFormProps) {
    const [rating, setRating] = useState(5);
    const [comment, setComment] = useState('');

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
                <div className="flex items-center mb-sm">
                    <label className="mr-md text-sm font-medium">별점</label>
                    <div className="flex gap-1">
                        {[1, 2, 3, 4, 5].map((star) => (
                            <button
                                key={star}
                                type="button"
                                onClick={() => setRating(star)}
                                className={`text-xl ${star <= rating ? 'text-yellow-500' : 'text-gray-300'}`}
                            >
                                ★
                            </button>
                        ))}
                    </div>
                </div>

                <div className="mb-sm">
                    <textarea
                        className="w-full p-sm border rounded resize-none text-sm focus:outline-none focus:border-primary"
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
