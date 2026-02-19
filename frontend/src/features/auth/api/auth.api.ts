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
        id: string; 
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
        // 1. CUSTOMER 로그인 시도
        try {
            const response = await client.post<LoginResponse>('/auth/customer/login', data);
            if (!response.user) {
                // @ts-ignore
                response.user = {
                    id: data.email,
                    email: data.email,
                    name: data.email.split('@')[0],
                    role: 'CUSTOMER'
                };
            }
            return response;
        } catch { /* fall through */ }

        // 2. OPERATOR 로그인 시도
        try {
            const response = await client.post<LoginResponse>('/auth/operator/login', data);
            if (!response.user) {
                // @ts-ignore
                response.user = {
                    id: data.email,
                    email: data.email,
                    name: data.email.split('@')[0],
                    role: 'OPERATOR'
                };
            }
            return response;
        } catch { /* fall through */ }

        // 3. PLATFORM_ADMIN 로그인 시도
        try {
            const response = await client.post<LoginResponse>('/auth/admin/login', data);
            if (!response.user) {
                // @ts-ignore
                response.user = {
                    id: data.email,
                    email: data.email,
                    name: data.email.split('@')[0],
                    role: 'PLATFORM_ADMIN'
                };
            }
            return response;
        } catch { /* fall through */ }

        throw new Error('로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요.');
    },

    signup: async (data: SignupData) => {
        return await client.post<string>('/auth/customer/signup', {
            email: data.email,
            password: data.password,
            name: data.name,
            phoneNumber: data.phoneNumber,
        });
    },

    signupOperator: async (data: SignupData) => {
        return await client.post<string>('/auth/operator/signup', {
            email: data.email,
            password: data.password,
            name: data.name,
        });
    },

    adminLogin: (data: LoginRequest) => {
        // Explicitly call logic or just alias login if user wants to use same flow
        // But if adminLogin is called explicitly, maybe direct to operator?
        return client.post<LoginResponse>('/auth/operator/login', data);
    }
};
