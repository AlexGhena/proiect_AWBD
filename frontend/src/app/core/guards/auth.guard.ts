import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from '../auth/auth.service';

// The access token is memory-only, so a page reload needs one restoreSession() attempt
// (via the remember-me cookie) before a protected route can be declared unreachable.
export const authGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  return authService
    .restoreSession()
    .pipe(
      map((user) => user !== null || router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } })),
    );
};
