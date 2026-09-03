import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from '../store/authStore';

describe('AuthStore', () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.setState({ token: null, user: null, orgType: null });
  });

  it('should set auth data on login', () => {
    const mockResponse = {
      userId: 'test-id',
      email: 'test@test.com',
      fullName: 'Test User',
      organisationId: 'org-id',
      organisationName: 'Test Org',
      orgType: 'BUYER' as const,
      roleCode: 'ADMIN',
      roleName: 'Admin',
      permissions: ['read', 'write'],
      accessToken: 'test-token',
      refreshToken: 'test-refresh',
    };

    useAuthStore.getState().setAuth(mockResponse);

    expect(useAuthStore.getState().token).toBe('test-token');
    expect(useAuthStore.getState().user?.email).toBe('test@test.com');
    expect(useAuthStore.getState().orgType).toBe('BUYER');
    expect(localStorage.getItem('token')).toBe('test-token');
  });

  it('should clear auth data on logout', () => {
    useAuthStore.getState().setAuth({
      userId: 'test-id', email: 'test@test.com', fullName: 'Test',
      organisationId: 'org-id', organisationName: 'Org', orgType: 'BUYER',
      roleCode: 'ADMIN', roleName: 'Admin', permissions: [],
      accessToken: 'token', refreshToken: 'refresh',
    });

    useAuthStore.getState().logout();

    expect(useAuthStore.getState().token).toBeNull();
    expect(useAuthStore.getState().user).toBeNull();
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('should switch organisation', () => {
    useAuthStore.getState().setAuth({
      userId: 'test-id', email: 'test@test.com', fullName: 'Test',
      organisationId: 'org-id', organisationName: 'Org', orgType: 'BUYER',
      roleCode: 'ADMIN', roleName: 'Admin', permissions: [],
      accessToken: 'token', refreshToken: 'refresh',
    });

    useAuthStore.getState().switchOrg('SUPPLIER');
    expect(useAuthStore.getState().orgType).toBe('SUPPLIER');
    expect(localStorage.getItem('orgType')).toBe('SUPPLIER');
  });
});
