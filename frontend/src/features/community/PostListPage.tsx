import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { communityApi, type Post } from './api/community.api';
import { useAuth } from '../../contexts/AuthContext';

export default function PostListPage() {
    const { user } = useAuth();
    const [posts, setPosts] = useState<Post[]>([]);
    const [loading, setLoading] = useState(false);
    const [filter, setFilter] = useState<'ALL' | 'NOTICE' | 'QNA' | 'GENERAL'>('ALL');

    useEffect(() => {
        loadPosts();
    }, []);

    const loadPosts = async () => {
        setLoading(true);
        try {
            const data = await communityApi.getPosts();
            setPosts(data);
        } catch (error) {
            console.error("Failed to load posts", error);
        } finally {
            setLoading(false);
        }
    };

    const filteredPosts = filter === 'ALL'
        ? posts
        : posts.filter(p => p.category === filter);

    return (
        <div className="container mx-auto p-md max-w-4xl">
            <div className="flex justify-between items-center mb-lg">
                <h1 className="text-3xl font-bold">커뮤니티</h1>
                {user && (
                    <Link to="/community/new" className="btn btn-primary">
                        글쓰기
                    </Link>
                )}
            </div>

            <div className="flex gap-md mb-md border-b">
                {['ALL', 'NOTICE', 'QNA', 'GENERAL'].map((cat) => (
                    <button
                        key={cat}
                        className={`pb-sm px-md ${filter === cat ? 'border-b-2 border-primary font-bold text-primary' : 'text-muted'}`}
                        onClick={() => setFilter(cat as any)}
                    >
                        {cat === 'ALL' ? '전체' : cat}
                    </button>
                ))}
            </div>

            {loading ? (
                <div className="text-center py-xl">Loading...</div>
            ) : (
                <div className="bg-white rounded-lg shadow-sm overflow-hidden">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-gray-50 border-b text-sm text-muted uppercase">
                                <th className="p-md font-medium w-16 text-center">No</th>
                                <th className="p-md font-medium w-24 text-center">분류</th>
                                <th className="p-md font-medium">제목</th>
                                <th className="p-md font-medium w-32 text-center">작성자</th>
                                <th className="p-md font-medium w-32 text-center">작성일</th>
                                <th className="p-md font-medium w-20 text-center">조회</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredPosts.length > 0 ? (
                                filteredPosts.map((post, index) => (
                                    <tr key={post.id} className="border-b hover:bg-gray-50">
                                        <td className="p-md text-center text-muted text-sm">{filteredPosts.length - index}</td>
                                        <td className="p-md text-center">
                                            <span className={`px-2 py-1 rounded text-xs font-bold ${post.category === 'NOTICE' ? 'bg-red-100 text-red-700' :
                                                post.category === 'QNA' ? 'bg-blue-100 text-blue-700' :
                                                    'bg-gray-100 text-gray-700'
                                                }`}>
                                                {post.category}
                                            </span>
                                        </td>
                                        <td className="p-md">
                                            <Link to={`/community/${post.id}`} className="hover:text-primary hover:underline font-medium">
                                                {post.title}
                                            </Link>
                                        </td>
                                        <td className="p-md text-center text-sm">{post.authorName}</td>
                                        <td className="p-md text-center text-sm text-muted">
                                            {new Date(post.createdAt).toLocaleDateString()}
                                        </td>
                                        <td className="p-md text-center text-sm text-muted">{post.views}</td>
                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td colSpan={6} className="p-xl text-center text-muted">
                                        게시글이 없습니다.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}
