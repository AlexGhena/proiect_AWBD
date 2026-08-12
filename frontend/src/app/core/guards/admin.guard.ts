import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from '../auth/auth.service';

// Same session-restore dance as authGuard (the access token is memory-only, so a reload needs one
// restoreSession() attempt before the route can be judged), plus a ROLE_ADMIN check on top.
export const adminGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const decide = () => {
    const user = authService.currentUser();
    if (!user) {
      return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }
    return user.roles.includes('ROLE_ADMIN') || router.createUrlTree(['/dashboard']);
  };

  if (authService.isAuthenticated()) {
    return decide();
  }

  return authService.restoreSession().pipe(map(() => decide()));
};
