import axios from "axios";
import type { AuthResponse } from "../types";

const AUTH_TOKEN_KEY = "access_token"
const AUTH_REFRESH_TOKEN_KEY = "refresh_token"

const baseURL = import.meta.env.VITE_API_URL || "http://localhost:8080/api/v1"

export const api = axios.create({
  baseURL,
  headers:{
    'Content-Type': 'application/json'
  },
  timeout: 10000
})

export const getAuthToken = (): string | null => {
  return window.localStorage.getItem(AUTH_TOKEN_KEY)
}

export const setAuthToken = (accessToken: string): void =>  {
  window.localStorage.setItem(AUTH_TOKEN_KEY, accessToken)
}

export const getRefreshToken = (): string | null => {
  return window.localStorage.getItem(AUTH_REFRESH_TOKEN_KEY)
}

export const setRefreshToken = (refreshToken: string): void => {
  window.localStorage.setItem(AUTH_REFRESH_TOKEN_KEY, refreshToken)
}

export const removeTokens = (): void => {
  window.localStorage.removeItem(AUTH_TOKEN_KEY);
  window.localStorage.removeItem(AUTH_REFRESH_TOKEN_KEY);
}

api.interceptors.request.use(
  (config) => {
    const accessToken = getAuthToken();

    if(accessToken && config.headers){
      config.headers.Authorization = `Bearer ${accessToken}`
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
)

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error?.config;
    const requestUrl: string = originalRequest?.url || "";
    const isAuthRoute =
      requestUrl.includes("/auth/login") ||
      requestUrl.includes("/auth/register") ||
      requestUrl.includes("/auth/logout") ||
      requestUrl.includes("/auth/refresh");

    if (error?.response?.status === 401 && !isAuthRoute && !originalRequest?._retry) {
      originalRequest._retry = true;

      const refreshToken = getRefreshToken();

      if (refreshToken) {
        try {
          const response = await axios.post<AuthResponse>(`${baseURL}/auth/refresh`, {
            refreshToken,
          });

          setAuthToken(response.data.authToken);
          setRefreshToken(response.data.refreshToken);

          originalRequest.headers.Authorization = `Bearer ${response.data.authToken}`;
          return api(originalRequest);
        } catch (refreshError) {
          removeTokens();
          window.dispatchEvent(new Event("auth:unauthorized"));
          return Promise.reject(refreshError);
        }
      } else {
        removeTokens();
        window.dispatchEvent(new Event("auth:unauthorized"));
      }
    }

    return Promise.reject(error?.response?.data ?? error);
  }
)

export default api;
