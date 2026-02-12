import { useState, useEffect, type FormEvent } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { communityApi, type Post, type Comment } from './api/community.api';
import { useAuth } from '../../contexts/AuthContext';

export default function PostDetailPage() {
    const { id } = useParams();
    const { user } = useAuth();
    const navigate = useNavigate();

    const [post, setPost] = useState<Post | null>(null);
    const [comments, setComments] = useState<Comment[]>([]);
    const [loading, setLoading] = useState(true);
    const [commentContent, setCommentContent] = useState('');

    useEffect(() => {
        if (id) {
            loadPostAndComments(id);
        }
    }, [id]);

    const loadPostAndComments = async (postId: string) => {
        setLoading(true);
        try {
            const postData = await communityApi.getPostById(postId);
            setPost(postData);

            const commentsData = await communityApi.getCommentsByPostId(postId);
            setComments(commentsData);
        } catch (error) {
            console.error("Failed to load post", error);
            alert('게시글을 찾을 수 없습니다.');
            navigate('/community');
        } finally {
            setLoading(false);
        }
    };

    const handleDeletePost = async () => {
        if (!post || !id) return;
        if (window.confirm('정말로 이 게시글을 삭제하시겠습니까?')) {
            try {
                await communityApi.deletePost(id);
                navigate('/community');
            } catch (error) {
                console.error("Failed to delete post", error);
            }
        }
    };

    const handleCommentSubmit = async (e: FormEvent) => {
        e.preventDefault();
        if (!user || !id || !commentContent.trim()) return;

        try {
            await communityApi.createComment({
                postId: id,
                content: commentContent,
                authorId: user.id,
                authorName: user.name,
            });
            setCommentContent('');
            // Reload comments
            const commentsData = await communityApi.getCommentsByPostId(id);
            setComments(commentsData);
        } catch (error) {
            console.error("Failed to add comment", error);
        }
    };

    const handleDeleteComment = async (commentId: string) => {
        if (window.confirm('댓글을 삭제하시겠습니까?')) {
            try {
                await communityApi.deleteComment(commentId);
                // Reload comments
                if (id) {
                    const commentsData = await communityApi.getCommentsByPostId(id);
                    setComments(commentsData);
                }
            } catch (error) {
                console.error("Failed to delete comment", error);
            }
        }
    };

    if (loading) return <div className="text-center py-xl">Loading...</div>;
    if (!post) return null;

    const isAuthor = user?.id === post.authorId || user?.role === 'ADMIN';

    return (
        <div className="container mx-auto p-md max-w-3xl">
            <Link to="/community" className="text-muted text-sm hover:underline mb-md inline-block">← 목록으로 돌아가기</Link>

            <div className="bg-white p-xl rounded-lg shadow-sm mb-lg">
                <div className="border-b pb-md mb-md">
                    <div className="flex justify-between items-start mb-sm">
                        <span className={`px-2 py-0.5 rounded text-xs font-bold ${post.category === 'NOTICE' ? 'bg-red-100 text-red-700' :
                                post.category === 'QNA' ? 'bg-blue-100 text-blue-700' :
                                    'bg-gray-100 text-gray-700'
                            }`}>
                            {post.category}
                        </span>
                        {isAuthor && (
                            <div className="flex gap-2 text-sm">
                                <Link to={`/community/${post.id}/edit`} className="text-gray-500 hover:text-primary">수정</Link>
                                <button onClick={handleDeletePost} className="text-gray-500 hover:text-red-500">삭제</button>
                            </div>
                        )}
                    </div>
                    <h1 className="text-2xl font-bold mb-xs">{post.title}</h1>
                    <div className="flex text-sm text-muted gap-md">
                        <span>{post.authorName}</span>
                        <span>{new Date(post.createdAt).toLocaleString()}</span>
                        <span>조회 {post.views}</span>
                    </div>
                </div>

                <div className="min-h-[200px] text-gray-800 whitespace-pre-wrap leading-relaxed">
                    {post.content}
                </div>
            </div>

            {/* Comments Section */}
            <div className="bg-gray-50 p-lg rounded-lg">
                <h3 className="font-bold mb-md">댓글 {comments.length}개</h3>

                <div className="space-y-md mb-lg">
                    {comments.map(comment => (
                        <div key={comment.id} className="border-b pb-sm last:border-0">
                            <div className="flex justify-between mb-xs">
                                <span className="font-bold text-sm">{comment.authorName}</span>
                                <span className="text-xs text-muted">{new Date(comment.createdAt).toLocaleString()}</span>
                            </div>
                            <p className="text-sm text-gray-700 mb-xs">{comment.content}</p>
                            {(user?.id === comment.authorId || user?.role === 'ADMIN') && (
                                <button
                                    onClick={() => handleDeleteComment(comment.id)}
                                    className="text-xs text-red-500 hover:underline"
                                >
                                    삭제
                                </button>
                            )}
                        </div>
                    ))}
                </div>

                {user ? (
                    <form onSubmit={handleCommentSubmit} className="flex gap-sm">
                        <input
                            type="text"
                            value={commentContent}
                            onChange={(e) => setCommentContent(e.target.value)}
                            className="flex-1 p-sm border rounded font-sm"
                            placeholder="댓글을 작성하세요..."
                            required
                        />
                        <button type="submit" className="btn btn-primary btn-sm whitespace-nowrap">
                            등록
                        </button>
                    </form>
                ) : (
                    <div className="text-center text-sm text-muted p-sm bg-gray-100 rounded">
                        댓글을 작성하려면 <Link to="/login" className="text-primary hover:underline">로그인</Link>이 필요합니다.
                    </div>
                )}
            </div>
        </div>
    );
}
