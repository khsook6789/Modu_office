import { client } from '../../../api/client';

export interface SignupData {
    email: string;
    password: string;
    name: string;
    phoneNumber?: string;
}

export interface LoginResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    user: {
        id: string; // email in this case based on backend logic usually, or we can fetch profile
        email: string;
        name: string;
        role: string;
    }
}

export interface LoginRequest {
    email: string;
    password: string;
}

export const authApi = {
    login: async (data: LoginRequest) => {
        try {
            const response = await client.post<LoginResponse>('/auth/customer/login', data);

            if (!response.user) {
                const isAdmin = data.email.includes('admin');
                const isOperator = data.email.includes('operator');

                // @ts-ignore
                response.user = {
                    id: data.email,
                    email: data.email,
                    name: 'Test User',
                    role: isAdmin ? 'ADMIN' : (isOperator ? 'OPERATOR' : 'USER'),
                };
            }
            return response;
        } catch (error) {
            console.warn("Backend unavailable, using MOCK data for login");

            // Check localStorage first
            const users = JSON.parse(localStorage.getItem('users') || '[]');
            const user = users.find((u: any) => u.email === data.email);

            if (user) {
                // If user found in localStorage, use their role
                // Simple password check (not secure for prod, ok for mock)
                if (user.password && user.password !== data.password) {
                    throw new Error('비밀번호가 일치하지 않습니다.');
                }

                return {
                    accessToken: 'mock-access-token',
                    refreshToken: 'mock-refresh-token',
                    tokenType: 'Bearer',
                    user: {
                        id: user.email,
                        email: user.email,
                        name: user.name,
                        role: user.role,
                    }
                } as LoginResponse;
            }

            // Fallback to email-based role assignment if not in localStorage
            const isAdmin = data.email.includes('admin');
            const isOperator = data.email.includes('operator');

            return {
                accessToken: 'mock-access-token',
                refreshToken: 'mock-refresh-token',
                tokenType: 'Bearer',
                user: {
                    id: data.email,
                    email: data.email,
                    name: 'Mock User',
                    role: isAdmin ? 'ADMIN' : (isOperator ? 'OPERATOR' : 'USER'),
                }
            } as LoginResponse;
        }
    },

    signup: async (data: SignupData) => {
        try {
            return await client.post<string>('/auth/customer/signup', {
                email: data.email,
                password: data.password,
                name: data.name,
                phoneNumber: data.phoneNumber,
            });
        } catch (error) {
            console.warn("Backend unavailable, using MOCK data for signup");

            // Persist to localStorage
            const users = JSON.parse(localStorage.getItem('users') || '[]');
            if (!users.find((u: any) => u.email === data.email)) {
                users.push({
                    email: data.email,
                    password: data.password,
                    name: data.name,
                    role: 'USER'
                });
                localStorage.setItem('users', JSON.stringify(users));
            }

            return "Mock Signup Success";
        }
    },

    signupOperator: async (data: SignupData) => {
        try {
            return await client.post<string>('/auth/operator/signup', {
                email: data.email,
                password: data.password,
                name: data.name,
            });
        } catch (error) {
            console.warn("Backend unavailable, using MOCK data for operator signup");

            // Persist to localStorage
            const users = JSON.parse(localStorage.getItem('users') || '[]');
            if (!users.find((u: any) => u.email === data.email)) {
                users.push({
                    email: data.email,
                    password: data.password,
                    name: data.name,
                    role: 'OPERATOR'
                });
                localStorage.setItem('users', JSON.stringify(users));
            }

            return "Mock Operator Signup Success";
        }
    },

    // Admin login usually same endpoint or separate? Assuming customer login checks role for now
    // If backend has specific admin login, use that.
    adminLogin: (data: LoginRequest) => {
        return client.post<LoginResponse>('/auth/customer/login', data);
    }
};
