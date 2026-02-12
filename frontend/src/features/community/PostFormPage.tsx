import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { communityApi } from './api/community.api';
import { useAuth } from '../../contexts/AuthContext';

export default function PostFormPage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const { id } = useParams(); // If ID exists, it's edit mode

    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [category, setCategory] = useState<'GENERAL' | 'QNA' | 'NOTICE'>('GENERAL');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!user) {
            alert('로그인이 필요합니다.');
            navigate('/login');
            return;
        }

        if (id) {
            loadPost(id);
        }
    }, [id, user]);

    const loadPost = async (postId: string) => {
        try {
            const post = await communityApi.getPostById(postId);
            if (user?.id !== post.authorId && user?.role !== 'ADMIN') {
                alert('수정 권한이 없습니다.');
                navigate('/community');
                return;
            }
            setTitle(post.title);
            setContent(post.content);
            setCategory(post.category);
        } catch (error) {
            console.error("Failed to load post", error);
            navigate('/community');
        }
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        if (!user) return;

        setLoading(true);
        try {
            if (id) {
                // Edit
                await communityApi.updatePost(id, { title, content, category });
            } else {
                // Create
                await communityApi.createPost({
                    title,
                    content,
                    category,
                    authorId: user.id,
                    authorName: user.name,
                });
            }
            navigate('/community');
        } catch (error) {
            console.error("Failed to save post", error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container mx-auto p-md max-w-2xl">
            <h1 className="text-3xl font-bold mb-lg">{id ? '게시글 수정' : '새 게시글 작성'}</h1>

            <form onSubmit={handleSubmit} className="bg-white p-lg rounded-lg shadow-sm">
                <div className="mb-md">
                    <label className="block text-sm font-medium mb-xs">카테고리</label>
                    <select
                        value={category}
                        onChange={(e) => setCategory(e.target.value as any)}
                        className="w-full p-sm border rounded"
                        disabled={user?.role !== 'ADMIN' && category === 'NOTICE'} // Only admin can create notices
                    >
                        <option value="GENERAL">일반</option>
                        <option value="QNA">Q&A</option>
                        {user?.role === 'ADMIN' && <option value="NOTICE">공지사항</option>}
                    </select>
                </div>

                <div className="mb-md">
                    <label className="block text-sm font-medium mb-xs">제목</label>
                    <input
                        type="text"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        className="w-full p-sm border rounded"
                        placeholder="제목을 입력하세요"
                        required
                    />
                </div>

                <div className="mb-lg">
                    <label className="block text-sm font-medium mb-xs">내용</label>
                    <textarea
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        className="w-full p-sm border rounded resize-none h-64"
                        placeholder="내용을 입력하세요"
                        required
                    />
                </div>

                <div className="flex justify-end gap-sm">
                    <button
                        type="button"
                        onClick={() => navigate('/community')}
                        className="btn btn-secondary"
                    >
                        취소
                    </button>
                    <button
                        type="submit"
                        className="btn btn-primary"
                        disabled={loading}
                    >
                        {loading ? '저장 중...' : (id ? '수정하기' : '등록하기')}
                    </button>
                </div>
            </form>
        </div>
    );
}
