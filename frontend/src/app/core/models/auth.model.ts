export type Role = 'ROLE_USER' | 'ROLE_ADMIN';

export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface LoginRequest {
  username: string;
  password: string;
  rememberMe: boolean;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

// Mirrors userService's UserResponse: the account is created disabled and PENDING, and only
// becomes a usable client after an administrator approves it.
export interface RegisterResponse {
  id: string;
  username: string;
  email: string;
  enabled: boolean;
  approvalStatus: ApprovalStatus;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
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
