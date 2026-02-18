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
        try {
            // 1. Try Customer Login
            const response = await client.post<LoginResponse>('/auth/customer/login', data);
            
            // If backend doesn't return user info (only token), construct it
            if (!response.user) {
                // @ts-ignore
                response.user = {
                    id: data.email,
                    email: data.email,
                    name: data.email.split('@')[0],
                    role: 'USER'
                };
            }
            return response;
        } catch (error) {
            console.warn("Customer login failed, attempting Operator login...", error);
            try {
                // 2. Try Operator/Admin Login
                // If this succeeds, the user is an Operator (or Admin)
                const response = await client.post<LoginResponse>('/auth/operator/login', data);
                
                if (!response.user) {
                    // Check if they are potentially a super admin by email convention, otherwise Operator
                    const isAdmin = data.email.includes('admin');
                    // @ts-ignore
                    response.user = {
                        id: data.email,
                        email: data.email,
                        name: data.email.split('@')[0],
                        role: isAdmin ? 'ADMIN' : 'OPERATOR'
                    };
                }
                return response;
            } catch (opError) {
                console.error("Operator login also failed", opError);
                throw new Error("로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요.");
            }
        }
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
