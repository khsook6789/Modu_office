export const BASE_URL = "http://localhost:8080/api";

interface RequestOptions extends RequestInit {
  token?: string;
}

class ApiClient {
  private async request<T>(
    endpoint: string,
    options: RequestOptions = {},
  ): Promise<T> {
    const url = `${BASE_URL}${endpoint}`;
    const headers: HeadersInit = {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    };

    const token = localStorage.getItem("token");
    if (token) {
      (headers as any)["Authorization"] = `Bearer ${token}`;
    }

    const config: RequestInit = {
      ...options,
      headers,
    };

    try {
      const response = await fetch(url, config);

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `API Error: ${response.status}`);
      }

      // Handle successful empty response (e.g. 204 No Content)
      if (response.status === 204) {
        return {} as unknown as T;
      }

      // Check if response has body
      const text = await response.text();
      let data: any;
      try {
        data = text ? JSON.parse(text) : {};
      } catch (jsonError) {
        // If parsing fails, treat it as a plain text string
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

  delete<T>(endpoint: string, options?: RequestOptions) {
    return this.request<T>(endpoint, { ...options, method: "DELETE" });
  }
}

export const client = new ApiClient();
