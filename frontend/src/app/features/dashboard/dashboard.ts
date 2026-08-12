import { Component, inject } from '@angular/core';

import { AuthService } from '../../core/auth/auth.service';
import { Topbar } from '../../layout/topbar/topbar';

@Component({
  selector: 'app-dashboard',
  imports: [Topbar],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly authService = inject(AuthService);

  protected readonly currentUser = this.authService.currentUser;
}
