import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../api/client';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('API Services', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should call login endpoint', async () => {
    const mockResponse = { data: { accessToken: 'token', fullName: 'Test' } };
    vi.mocked(api.post).mockResolvedValue(mockResponse);

    const { authApi } = await import('../api/services');
    const result = await authApi.login('test@test.com', 'password');

    expect(api.post).toHaveBeenCalledWith('/auth/login', {
      email: 'test@test.com',
      password: 'password',
    });
    expect(result.data.accessToken).toBe('token');
  });

  it('should call dashboard stats endpoint', async () => {
    const mockResponse = { data: { vehicles: 5, jobs: 3 } };
    vi.mocked(api.get).mockResolvedValue(mockResponse);

    const { dashboardApi } = await import('../api/services');
    const result = await dashboardApi.stats();

    expect(api.get).toHaveBeenCalledWith('/dashboard/stats');
    expect(result.data.vehicles).toBe(5);
  });

  it('should call vehicle list endpoint', async () => {
    const mockResponse = { data: [{ id: '1', registration: 'ABC123' }] };
    vi.mocked(api.get).mockResolvedValue(mockResponse);

    const { vehicleApi } = await import('../api/services');
    const result = await vehicleApi.list();

    expect(api.get).toHaveBeenCalledWith('/vehicles');
    expect(result.data).toHaveLength(1);
  });
});
