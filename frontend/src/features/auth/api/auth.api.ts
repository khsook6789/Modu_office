import { client, BASE_URL } from '../../../api/client';

export interface SignupData {
    email: string;
    password: string;
    name: string;
    phoneNumber?: string;
}

export interface TokenResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
}

export interface UserProfile {
    id: number;
    email: string;
    name: string;
    role: 'USER' | 'MANAGER' | 'ADMIN';
}

export interface LoginResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    user: UserProfile;
}

export interface LoginRequest {
    email: string;
    password: string;
}

// accessToken으로 /api/users/me를 호출해 사용자 정보 조회
async function fetchUserProfile(accessToken: string): Promise<UserProfile> {
    const response = await fetch(`${BASE_URL}/users/me`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`,
        },
    });
    if (!response.ok) throw new Error('프로필 조회 실패');
    const json = await response.json();
    // 백엔드 ApiResponse wrapper: { status, message, data }
    return json.data ?? json;
}

async function tryLogin(endpoint: string, data: LoginRequest): Promise<LoginResponse | null> {
    try {
        const tokenRes = await client.post<TokenResponse>(endpoint, data);
        if (!tokenRes?.accessToken) return null;

        const user = await fetchUserProfile(tokenRes.accessToken);
        return { ...tokenRes, user };
    } catch {
        return null;
    }
}

export const authApi = {
    login: async (data: LoginRequest): Promise<LoginResponse> => {
        // USER → MANAGER → ADMIN 순서로 시도
        const endpoints = [
            '/auth/user/login',
            '/auth/manager/login',
            '/auth/admin/login',
        ];

        for (const endpoint of endpoints) {
            const result = await tryLogin(endpoint, data);
            if (result) return result;
        }

        throw new Error('로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요.');
    },

    signup: async (data: SignupData) => {
        return await client.post<string>('/auth/user/signup', {
            email: data.email,
            password: data.password,
            name: data.name,
            phoneNumber: data.phoneNumber,
        });
    },

    signupOperator: async (data: SignupData) => {
        return await client.post<string>('/auth/manager/signup', {
            email: data.email,
            password: data.password,
            name: data.name,
        });
    },
};
