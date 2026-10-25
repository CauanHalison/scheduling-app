import type { AuthRequest, AuthResponse, ProviderResponse, RefreshTokenRequest, RegisterProviderRequest } from "../types";
import api, { setAuthToken, setRefreshToken } from "./api";

export const authService = {
  async register(payload: RegisterProviderRequest): Promise<void> {
    await api.post("/auth/register", payload);
  },
  
  async login(payload: AuthRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>("/auth/login", payload)
    setAuthToken(response.data.authToken)
    setRefreshToken(response.data.refreshToken)

    return response.data;
  },

  async refreshToken(payload: RefreshTokenRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>("/auth/refresh", payload);
    
    setAuthToken(response.data.authToken)
    setRefreshToken(response.data.refreshToken)

    return response.data;
  },

  async getMe(): Promise<ProviderResponse> {
    const response = await api.get<ProviderResponse>("/auth/me");

    return response.data
  },

  async logout(payload: RefreshTokenRequest): Promise<void> {
    await api.post("/auth/logout", payload);
  }
}

export default authService