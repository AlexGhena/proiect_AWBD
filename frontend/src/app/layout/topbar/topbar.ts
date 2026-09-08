import { Component, inject, input } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

export type TopbarSection = 'dashboard' | 'accounts' | 'cards' | 'transactions' | 'profile' | 'admin';

@Component({
  selector: 'app-topbar',
  imports: [RouterLink],
  templateUrl: './topbar.html',
  styleUrl: './topbar.scss',
})
export class Topbar {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly active = input<TopbarSection>('dashboard');

  protected readonly currentUser = this.authService.currentUser;

  protected get isAdmin(): boolean {
    return this.currentUser()?.roles.includes('ROLE_ADMIN') ?? false;
  }

  protected logout(): void {
    this.authService.logout().subscribe({
      complete: () => this.router.navigateByUrl('/login'),
      error: () => this.router.navigateByUrl('/login'),
    });
  }
}
