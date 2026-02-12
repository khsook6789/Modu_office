
export interface Post {
    id: string;
    title: string;
    content: string;
    authorId: string;
    authorName: string;
    createdAt: string;
    views: number;
    category: 'GENERAL' | 'QNA' | 'NOTICE';
}

export interface Comment {
    id: string;
    postId: string;
    content: string;
    authorId: string;
    authorName: string;
    createdAt: string;
}

const POSTS_STORAGE_KEY = 'community_posts';
const COMMENTS_STORAGE_KEY = 'community_comments';

// Seed initial posts
const INITIAL_POSTS: Post[] = [
    {
        id: '1',
        title: 'Welcome to Modu Office Community!',
        content: 'Feel free to ask questions and share your experiences here.',
        authorId: 'admin',
        authorName: 'Admin',
        createdAt: new Date().toISOString(),
        views: 120,
        category: 'NOTICE'
    },
    {
        id: '2',
        title: 'Review for Galaxy Conference Hall',
        content: 'It was great! Highly recommended.',
        authorId: 'user1',
        authorName: 'User1',
        createdAt: new Date(Date.now() - 86400000).toISOString(),
        views: 45,
        category: 'GENERAL'
    }
];

const getStoredPosts = (): Post[] => {
    const stored = localStorage.getItem(POSTS_STORAGE_KEY);
    if (!stored) {
        localStorage.setItem(POSTS_STORAGE_KEY, JSON.stringify(INITIAL_POSTS));
        return INITIAL_POSTS;
    }
    return JSON.parse(stored);
};

const getStoredComments = (): Comment[] => {
    const stored = localStorage.getItem(COMMENTS_STORAGE_KEY);
    return stored ? JSON.parse(stored) : [];
};

export const communityApi = {
    getPosts: (): Promise<Post[]> => {
        return new Promise((resolve) => {
            setTimeout(() => {
                const posts = getStoredPosts();
                // Sort by date desc
                posts.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
                resolve(posts);
            }, 300);
        });
    },

    getPostById: (id: string): Promise<Post> => {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                const posts = getStoredPosts();
                const post = posts.find(p => p.id === id);
                if (post) {
                    // Increment views
                    post.views += 1;
                    localStorage.setItem(POSTS_STORAGE_KEY, JSON.stringify(posts));
                    resolve(post);
                } else {
                    reject(new Error('Post not found'));
                }
            }, 300);
        });
    },

    createPost: (data: Omit<Post, 'id' | 'createdAt' | 'views'>): Promise<Post> => {
        return new Promise((resolve) => {
            setTimeout(() => {
                const posts = getStoredPosts();
                const newPost: Post = {
                    ...data,
                    id: Date.now().toString(),
                    createdAt: new Date().toISOString(),
                    views: 0
                };
                localStorage.setItem(POSTS_STORAGE_KEY, JSON.stringify([newPost, ...posts]));
                resolve(newPost);
            }, 500);
        });
    },

    updatePost: (id: string, data: Partial<Post>): Promise<Post> => {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                const posts = getStoredPosts();
                const index = posts.findIndex(p => p.id === id);
                if (index !== -1) {
                    const updatedPost = { ...posts[index], ...data };
                    posts[index] = updatedPost;
                    localStorage.setItem(POSTS_STORAGE_KEY, JSON.stringify(posts));
                    resolve(updatedPost);
                } else {
                    reject(new Error('Post not found'));
                }
            }, 500);
        });
    },

    deletePost: (id: string): Promise<void> => {
        return new Promise((resolve) => {
            setTimeout(() => {
                const posts = getStoredPosts();
                const newPosts = posts.filter(p => p.id !== id);
                localStorage.setItem(POSTS_STORAGE_KEY, JSON.stringify(newPosts));
                resolve();
            }, 300);
        });
    },

    // Comments
    getCommentsByPostId: (postId: string): Promise<Comment[]> => {
        return new Promise((resolve) => {
            setTimeout(() => {
                const comments = getStoredComments();
                const postComments = comments.filter(c => c.postId === postId);
                postComments.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()); // Ascending (oldest first)
                resolve(postComments);
            }, 300);
        });
    },

    createComment: (data: Omit<Comment, 'id' | 'createdAt'>): Promise<Comment> => {
        return new Promise((resolve) => {
            setTimeout(() => {
                const comments = getStoredComments();
                const newComment: Comment = {
                    ...data,
                    id: Date.now().toString(),
                    createdAt: new Date().toISOString()
                };
                localStorage.setItem(COMMENTS_STORAGE_KEY, JSON.stringify([...comments, newComment]));
                resolve(newComment);
            }, 300);
        });
    },

    deleteComment: (id: string): Promise<void> => {
        return new Promise((resolve) => {
            setTimeout(() => {
                const comments = getStoredComments();
                const newComments = comments.filter(c => c.id !== id);
                localStorage.setItem(COMMENTS_STORAGE_KEY, JSON.stringify(newComments));
                resolve();
            }, 300);
        });
    }
};
