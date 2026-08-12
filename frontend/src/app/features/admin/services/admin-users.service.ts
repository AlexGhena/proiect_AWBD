import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AdminUser,
  PageResponse,
  RoleSummary,
  RolesPageResponse,
  UserApprovalResponse,
} from '../../../core/models/admin.model';

export interface PageQuery {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'asc' | 'desc';
}

const USERS_BASE = '/api/users';
const ROLES_BASE = '/api/roles';

function toParams(query: PageQuery): HttpParams {
  let params = new HttpParams();
  if (query.page !== undefined) params = params.set('page', query.page);
  if (query.size !== undefined) params = params.set('size', query.size);
  if (query.sortBy) params = params.set('sortBy', query.sortBy);
  if (query.sortDirection) params = params.set('sortDirection', query.sortDirection);
  return params;
}

@Injectable({ providedIn: 'root' })
export class AdminUsersService {
  private readonly http = inject(HttpClient);

  listUsers(query: PageQuery = {}): Observable<PageResponse<AdminUser>> {
    return this.http.get<PageResponse<AdminUser>>(USERS_BASE, { params: toParams(query) });
  }

  listPending(query: PageQuery = {}): Observable<PageResponse<AdminUser>> {
    return this.http.get<PageResponse<AdminUser>>(`${USERS_BASE}/pending`, { params: toParams(query) });
  }

  listDeleted(query: PageQuery = {}): Observable<PageResponse<AdminUser>> {
    return this.http.get<PageResponse<AdminUser>>(`${USERS_BASE}/deleted`, { params: toParams(query) });
  }

  approve(userId: string): Observable<UserApprovalResponse> {
    return this.http.post<UserApprovalResponse>(`${USERS_BASE}/${userId}/approve`, {});
  }

  reject(userId: string): Observable<AdminUser> {
    return this.http.post<AdminUser>(`${USERS_BASE}/${userId}/reject`, {});
  }

  // Soft-delete: disables login but keeps the row (see userService's AppUser.deletedAt).
  deactivate(userId: string): Observable<void> {
    return this.http.delete<void>(`${USERS_BASE}/${userId}`);
  }

  listRoles(query: PageQuery = {}): Observable<RolesPageResponse> {
    return this.http.get<RolesPageResponse>(ROLES_BASE, { params: toParams(query) });
  }

  listRolesForUser(userId: string): Observable<RoleSummary[]> {
    return this.http.get<RoleSummary[]>(`${USERS_BASE}/${userId}/roles`);
  }
}
