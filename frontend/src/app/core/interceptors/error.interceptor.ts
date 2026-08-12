import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { UNAUTHENTICATED_PATHS } from './auth.interceptor';

// Cross-cutting navigation only: a 401 on an already-authenticated call means the session is
// no longer valid, and a 5xx/network failure means the app can't function — anything else
// (400/403/404/409…) is left for the calling component to interpret.
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && req.url.startsWith('/api/')) {
        const isAuthEndpoint = UNAUTHENTICATED_PATHS.some((path) => req.url.startsWith(path));

        if (error.status === 401 && !isAuthEndpoint) {
          authService.clearSession();
          router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
        } else if (error.status === 0 || error.status >= 500) {
          router.navigate(['/500']);
        }
      }

      return throwError(() => error);
    }),
  );
};
