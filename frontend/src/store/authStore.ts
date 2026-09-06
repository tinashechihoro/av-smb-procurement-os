import { create } from 'zustand';
import type { AuthResponse } from '../types';

interface AuthState {
  token: string | null;
  user: AuthResponse | null;
  orgType: 'BUYER' | 'SUPPLIER' | null;
  setAuth: (response: AuthResponse) => void;
  logout: () => void;
  switchOrg: (orgType: 'BUYER' | 'SUPPLIER') => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('token'),
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  orgType: (localStorage.getItem('orgType') as 'BUYER' | 'SUPPLIER') || null,

  setAuth: (response) => {
    localStorage.setItem('token', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('user', JSON.stringify(response));
    localStorage.setItem('orgType', response.orgType);
    set({
      token: response.accessToken,
      user: response,
      orgType: response.orgType,
    });
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    localStorage.removeItem('orgType');
    set({ token: null, user: null, orgType: null });
  },

  switchOrg: (orgType) => {
    localStorage.setItem('orgType', orgType);
    set({ orgType });
  },
}));
