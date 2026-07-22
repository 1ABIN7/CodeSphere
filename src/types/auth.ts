export enum Role {
  SUPER_ADMIN = 'ROLE_SUPER_ADMIN',
  ORG_ADMIN = 'ROLE_ORG_ADMIN',
  EXAMINER = 'ROLE_EXAMINER',
  INSTRUCTOR = 'ROLE_INSTRUCTOR',
  CANDIDATE = 'ROLE_CANDIDATE',
}

export interface User {
  id?: number;
  username: string;
  email?: string;
  role: Role;
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  username: string;
  role: Role;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}
