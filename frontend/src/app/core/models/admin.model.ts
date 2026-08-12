import { ApprovalStatus } from './auth.model';

// Mirrors userService's UserResponse.
export interface AdminUser {
  id: string;
  username: string;
  email: string;
  enabled: boolean;
  approvalStatus: ApprovalStatus;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
}

export interface UserApprovalResponse {
  user: AdminUser;
  iban: string;
}

export interface RoleSummary {
  id: string;
  name: string;
  description: string | null;
}

// The flat envelope used by /api/users, /api/users/pending, /api/users/deleted.
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  sortBy: string;
  sortDirection: string;
}

// /api/roles uses Spring Data's PagedModel instead, with page metadata nested differently.
export interface RolesPageResponse {
  content: RoleSummary[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
