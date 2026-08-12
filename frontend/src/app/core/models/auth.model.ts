export type Role = 'ROLE_USER' | 'ROLE_ADMIN';

export interface LoginRequest {
  username: string;
  password: string;
  rememberMe: boolean;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  userId: string;
  username: string;
  roles: Role[];
}

export interface CurrentUser {
  userId: string;
  username: string;
  roles: Role[];
}
