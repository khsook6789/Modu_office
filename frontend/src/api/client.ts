export const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

interface RequestOptions extends RequestInit {
  token?: string;
  params?: Record<string, any>;
  _isRetry?: boolean; // 무한 루프 방지용 내부 플래그
}

// 역할별 refresh 엔드포인트 매핑
function getRefreshEndpoint(): string {
  try {
    const user = JSON.parse(localStorage.getItem("user") || "{}");
    const role = user?.role as string;
    if (role === "MANAGER") return "/auth/manager/refresh";
    if (role === "ADMIN") return "/auth/admin/refresh";
  } catch {}
  return "/auth/user/refresh";
}

// refreshToken으로 새 accessToken 발급. 성공시 true, 실패시 false
async function tryRefreshToken(): Promise<boolean> {
  const refreshToken = localStorage.getItem("refreshToken");
  if (!refreshToken) return false;

  try {
    const endpoint = getRefreshEndpoint();
    const res = await fetch(`${BASE_URL}${endpoint}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });

    if (!res.ok) return false;

    const data = await res.json();
    // 백엔드 TokenResponse: { accessToken, refreshToken, tokenType }
    const newAccessToken = data.accessToken ?? data.data?.accessToken;
    const newRefreshToken = data.refreshToken ?? data.data?.refreshToken;

    if (!newAccessToken) return false;

    localStorage.setItem("token", newAccessToken);
    if (newRefreshToken) {
      localStorage.setItem("refreshToken", newRefreshToken);
    }
    return true;
  } catch {
    return false;
  }
}

function clearAuthAndRedirect() {
  localStorage.removeItem("token");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");

  // 무한 리다이렉트 방지: 이미 로그인 페이지면 이동하지 않음
  if (window.location.pathname === "/login") {
    return;
  }
  window.location.href = "/login";
}

class ApiClient {
  private async request<T>(
    endpoint: string,
    options: RequestOptions = {},
  ): Promise<T> {
    let url = `${BASE_URL}${endpoint}`;

    // Handle Query Params
    if (options.params) {
      const queryString = new URLSearchParams(
        Object.entries(options.params).reduce(
          (acc, [key, value]) => {
            if (value !== undefined && value !== null) {
              acc[key] = String(value);
            }
            return acc;
          },
          {} as Record<string, string>,
        ),
      ).toString();
      if (queryString) {
        url += `?${queryString}`;
      }
    }

    const headers: HeadersInit = {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    };

    const token = localStorage.getItem("token");
    if (token) {
      // @ts-ignore
      headers["Authorization"] = `Bearer ${token}`;
    }

    const { _isRetry, params, ...fetchOptions } = options;
    const config: RequestInit = {
      ...fetchOptions,
      headers,
    };

    try {
      const response = await fetch(url, config);

      if (!response.ok) {
        if (response.status === 401) {
          // 이미 재시도였으면 → 로그아웃
          if (_isRetry) {
            clearAuthAndRedirect();
            throw new Error("세션이 만료되었습니다. 다시 로그인해 주세요.");
          }

          // refreshToken으로 갱신 시도
          const refreshed = await tryRefreshToken();
          if (!refreshed) {
            clearAuthAndRedirect();
            throw new Error("세션이 만료되었습니다. 다시 로그인해 주세요.");
          }

          // 갱신 성공 → 원래 요청 재시도 (_isRetry 플래그로 무한루프 방지)
          return this.request<T>(endpoint, { ...options, _isRetry: true });
        }

        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `API Error: ${response.status}`);
      }

      if (response.status === 204) {
        return {} as unknown as T;
      }

      const text = await response.text();
      let data: any;
      try {
        data = text ? JSON.parse(text) : {};
      } catch (jsonError) {
        console.warn(`[API Client] Non-JSON response for ${endpoint}:`, text);
        data = text;
      }

      console.log(`[API Response] ${endpoint}:`, data);
      return data;
    } catch (error) {
      console.error("API Request Failed:", error);
      throw error;
    }
  }

  get<T>(endpoint: string, options?: RequestOptions) {
    return this.request<T>(endpoint, { ...options, method: "GET" });
  }

  post<T>(endpoint: string, data: any, options?: RequestOptions) {
    return this.request<T>(endpoint, {
      ...options,
      method: "POST",
      body: JSON.stringify(data),
    });
  }

  put<T>(endpoint: string, data: any, options?: RequestOptions) {
    return this.request<T>(endpoint, {
      ...options,
      method: "PUT",
      body: JSON.stringify(data),
    });
  }

  patch<T>(endpoint: string, data?: any, options?: RequestOptions) {
    return this.request<T>(endpoint, {
      ...options,
      method: "PATCH",
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  delete<T>(endpoint: string, options?: RequestOptions) {
    return this.request<T>(endpoint, { ...options, method: "DELETE" });
  }
}

export const client = new ApiClient();
