import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'login',
    loadComponent: () => import('./features/authentication/login/login').then((m) => m.Login),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: '500',
    loadComponent: () =>
      import('./features/errors/server-error/server-error').then((m) => m.ServerError),
  },
  {
    path: '**',
    loadComponent: () => import('./features/errors/not-found/not-found').then((m) => m.NotFound),
  },
];
