import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import toast from 'react-hot-toast';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshPromise: Promise<string | null> | null = null;

// RBAC denials land here because list pages swallow load errors; one toast per
// burst keeps a dashboard of concurrent 403s from spamming the user.
let lastForbiddenToastAt = 0;
function notifyForbidden() {
  const now = Date.now();
  if (now - lastForbiddenToastAt > 4000) {
    lastForbiddenToastAt = now;
    toast.error('You do not have permission to view this data (SMB confidential)');
  }
}

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) return null;

  try {
    const { data } = await axios.post('/api/auth/refresh', { refreshToken });
    localStorage.setItem('token', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    return data.accessToken as string;
  } catch {
    return null;
  }
}

function forceLogout() {
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
  localStorage.removeItem('orgType');
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
}

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;
    const status = error.response?.status;
    const isAuthEndpoint = original?.url?.includes('/auth/refresh');

    if (status === 401 && original && !original._retry && !isAuthEndpoint) {
      original._retry = true;
      // Single-flight: concurrent 401s share one refresh call
      refreshPromise = refreshPromise ?? refreshAccessToken();
      try {
        const token = await refreshPromise;
        if (token) {
          original.headers.Authorization = `Bearer ${token}`;
          return api(original);
        }
      } finally {
        refreshPromise = null;
      }
      forceLogout();
    } else if (status === 401 && isAuthEndpoint) {
      forceLogout();
    } else if (status === 403 && !isAuthEndpoint) {
      notifyForbidden();
    }

    return Promise.reject(error);
  }
);

export default api;
