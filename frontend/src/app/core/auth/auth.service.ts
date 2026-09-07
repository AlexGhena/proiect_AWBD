import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, map, of, switchMap } from 'rxjs';

import { CurrentUser, LoginRequest, LoginResponse, RegisterRequest, RegisterResponse } from '../models/auth.model';
import { TokenStore } from './token-store';

const AUTH_BASE = '/api/auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenStore = inject(TokenStore);

  private readonly user = signal<CurrentUser | null>(null);

  readonly currentUser = this.user.asReadonly();
  readonly isAuthenticated = computed(() => this.user() !== null);

  login(request: LoginRequest): Observable<CurrentUser> {
    // The CSRF cookie must be primed before any state-changing request, including login itself.
    return this.http
      .get(`${AUTH_BASE}/csrf`)
      .pipe(
        switchMap(() => this.http.post<LoginResponse>(`${AUTH_BASE}/login`, request)),
        map((response) => this.applySession(response)),
      );
  }

  // The created account is disabled and PENDING - there is no session to start until an admin
  // approves it, so this never touches the token store.
  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http
      .get(`${AUTH_BASE}/csrf`)
      .pipe(switchMap(() => this.http.post<RegisterResponse>(`${AUTH_BASE}/register`, request)));
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(`${AUTH_BASE}/logout`, {})
      .pipe(finalize(() => this.clearSession()));
  }

  // Exchanges a still-valid remember-me cookie for a fresh access token, e.g. after a page reload.
  // The CSRF cookie must be primed first: authenticating via the remember-me cookie rotates the
  // CSRF token server-side, so after a reload there is no valid XSRF-TOKEN cookie for the client to
  // echo, and POST /token would otherwise be rejected with 403 before the remember-me cookie is read.
  restoreSession(): Observable<CurrentUser | null> {
    return this.http.get(`${AUTH_BASE}/csrf`).pipe(
      switchMap(() => this.http.post<LoginResponse>(`${AUTH_BASE}/token`, {})),
      map((response) => this.applySession(response)),
      catchError(() => {
        this.clearSession();
        return of(null);
      }),
    );
  }

  private applySession(response: LoginResponse): CurrentUser {
    this.tokenStore.setAccessToken(response.accessToken);
    const user: CurrentUser = {
      userId: response.userId,
      username: response.username,
      roles: response.roles,
    };
    this.user.set(user);
    return user;
  }

  // Also used by the global error interceptor to drop a session the server no longer honors.
  clearSession(): void {
    this.tokenStore.clear();
    this.user.set(null);
  }
}
