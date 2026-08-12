import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { TokenStore } from '../auth/token-store';

// Endpoints that are expected to 401 as part of normal operation (bad credentials, no
// remember-me cookie yet) — the global error interceptor must not treat these as a session
// expiry and redirect away from them.
export const UNAUTHENTICATED_PATHS = ['/api/auth/csrf', '/api/auth/login', '/api/auth/token'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/')) {
    return next(req);
  }

  const tokenStore = inject(TokenStore);
  const accessToken = tokenStore.accessToken();
  const attachBearer = accessToken && !UNAUTHENTICATED_PATHS.some((path) => req.url.startsWith(path));

  // withCredentials so the CSRF and remember-me cookies still flow once this isn't served
  // through the dev proxy and calls become cross-origin (backend CORS allows credentials).
  return next(
    req.clone({
      withCredentials: true,
      ...(attachBearer ? { setHeaders: { Authorization: `Bearer ${accessToken}` } } : {}),
    }),
  );
};
