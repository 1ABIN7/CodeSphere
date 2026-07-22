import client from './client';
import type { LoginRequest, RegisterRequest, AuthResponse, ForgotPasswordRequest, ResetPasswordRequest } from '@/types';

export const authApi = {
  login: (data: LoginRequest) => client.post<AuthResponse>('/auth/login', data),
  register: (data: RegisterRequest) => client.post<AuthResponse>('/auth/register', data),
  forgotPassword: (data: ForgotPasswordRequest) => client.post('/auth/forgot-password', data),
  resetPassword: (data: ResetPasswordRequest) => client.post('/auth/reset-password', data),
  verifyEmail: (token: string) => client.get(`/auth/verify-email?token=${token}`),
  refresh: (refreshToken: string) => client.post(`/auth/refresh?refreshToken=${refreshToken}`),
  logout: () => client.post('/auth/logout'),
};
