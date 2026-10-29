import { useCallback, useEffect, useState, type ReactNode } from "react";
import { queryClient } from "../lib/queryClient";
import type { AuthRequest, ProviderResponse, RegisterProviderRequest } from "../types";
import { getAuthToken, getRefreshToken, removeTokens } from "../service/api";
import authService from "../service/authService";
import { decodeToken } from "../utils/jwt";
import { AuthContext } from "./authContextDef";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [provider, setProvider] = useState<ProviderResponse | null>(() => {
    const token = getAuthToken();
    return token ? decodeToken(token) : null;
  });

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken();
    try {
      if (refreshToken) {
        await authService.logout({ refreshToken });
      }
    } finally {
      removeTokens();
      setProvider(null);
      queryClient.clear();
    }
  }, []);

  useEffect(() => {
    const handleUnauthorized = () => {
      removeTokens();
      setProvider(null);
      queryClient.clear();
    };

    window.addEventListener("auth:unauthorized", handleUnauthorized);
    return () => {
      window.removeEventListener("auth:unauthorized", handleUnauthorized);
    };
  }, []);

  const login = async (payload: AuthRequest) => {
    const response = await authService.login(payload);
    const user = decodeToken(response.authToken);
    setProvider(user);
  };

  const register = async (payload: RegisterProviderRequest) => {
    await authService.register(payload);
  };

  const refreshUser = useCallback(async () => {
    const token = getAuthToken();
    setProvider(token ? decodeToken(token) : null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        provider,
        loading: false,
        isAuthenticated: !!provider,
        login,
        register,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export default AuthProvider;
