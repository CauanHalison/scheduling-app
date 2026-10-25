export interface AuthRequest {
  email: string,
  password: string
}

export interface AuthResponse {
  authToken: string,
  refreshToken: string
}

export interface ProviderResponse {
  id: string,
  name: string,
  email: string,
  slug: string,
  timeZone: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface RegisterProviderRequest {
  name: string,
  email: string,
  password: string,
  slug: string,
  timeZone: string
}