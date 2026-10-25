import { createContext } from 'react';
import type { AuthRequest, ProviderResponse, RegisterProviderRequest } from '../types';

export interface AuthContextType {
  provider: ProviderResponse | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (payload: AuthRequest) => Promise<void>;
  register: (payload: RegisterProviderRequest) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);