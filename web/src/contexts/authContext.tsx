import { useCallback, useEffect, useState, type ReactNode } from "react";
import type { AuthRequest, ProviderResponse, RegisterProviderRequest } from "../types";
import { getAuthToken, getRefreshToken, removeTokens } from "../service/api";
import authService from "../service/authService";
import { AuthContext } from "./authContextDef";

export function AuthProvider({children}: {children: ReactNode}) {
  const [provider, setProvider] = useState<ProviderResponse | null>(null)
  const [loading, setLoading] = useState<boolean>(() => !!getAuthToken())

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken()
    try {
      if (refreshToken) {
        await authService.logout({ refreshToken })
      }
    } finally {
      removeTokens()
      setProvider(null)
    }
  }, [])

  const refreshUser = useCallback(async () => {
    const token = getAuthToken()
    if(!token) {
      setProvider(null)
      return
    }
    try {
      const currentProvider = await authService.getMe();
      setProvider(currentProvider)
    } catch {
      await logout()
      setProvider(null)
    }
  }, [logout])

  useEffect(() => {
    let active = true;

    const handleUnauthorized = () => {
      if (active) {
        removeTokens();
        setProvider(null);
      }
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);

    const token = getAuthToken();
    if (!token) {
      return () => {
        active = false;
        window.removeEventListener('auth:unauthorized', handleUnauthorized);
      };
    }

    authService.getMe().then((currentProvider) => {
      if(active) {
        setProvider(currentProvider)
      }
    }).catch(() => {
      if(active) {
        logout()
        setProvider(null)
      }
    }).finally(() => {
      if (active) {
        setLoading(false)
      }
    });

    return () => {
      active = false;
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, [logout])

  const login = async (payload: AuthRequest) => {
    await authService.login(payload);
    const currentProvider = await authService.getMe();
    setProvider(currentProvider);
  }

  const register = async (payload: RegisterProviderRequest) => {
    await authService.register(payload)
  }

  return (
    <AuthContext.Provider
      value={{
        provider,
        loading,
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
