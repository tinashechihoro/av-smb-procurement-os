import { create } from 'zustand';
import type { AuthResponse } from '../types';

interface AuthState {
  token: string | null;
  user: AuthResponse | null;
  orgType: 'BUYER' | 'SUPPLIER' | null;
  pendingLogin: PendingLogin | null;
  setAuth: (response: AuthResponse) => void;
  setPendingLogin: (pending: PendingLogin | null) => void;
  logout: () => void;
  switchOrg: (orgType: 'BUYER' | 'SUPPLIER') => void;
}

interface PendingLogin {
  userId: string;
  email: string;
  fullName: string;
  otpId?: string;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('token'),
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  orgType: (localStorage.getItem('orgType') as 'BUYER' | 'SUPPLIER') || null,
  pendingLogin: JSON.parse(localStorage.getItem('pendingLogin') || 'null'),

  setAuth: (response) => {
    localStorage.setItem('token', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('user', JSON.stringify(response));
    localStorage.setItem('orgType', response.orgType);
    localStorage.removeItem('pendingLogin');
    set({
      token: response.accessToken,
      user: response,
      orgType: response.orgType,
      pendingLogin: null,
    });
  },

  setPendingLogin: (pending) => {
    if (pending) {
      localStorage.setItem('pendingLogin', JSON.stringify(pending));
    } else {
      localStorage.removeItem('pendingLogin');
    }
    set({ pendingLogin: pending });
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    localStorage.removeItem('orgType');
    localStorage.removeItem('pendingLogin');
    set({ token: null, user: null, orgType: null, pendingLogin: null });
  },

  switchOrg: (orgType) => {
    localStorage.setItem('orgType', orgType);
    set({ orgType });
  },
}));
