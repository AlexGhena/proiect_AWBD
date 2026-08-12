import { Component, OnInit, inject, signal } from '@angular/core';

import { RoleSummary } from '../../../core/models/admin.model';
import { Pagination } from '../../../shared/components/pagination/pagination';
import { AdminUsersService } from '../services/admin-users.service';

@Component({
  selector: 'app-admin-roles-list',
  imports: [Pagination],
  templateUrl: './roles-list.html',
  styleUrl: '../admin-shared.scss',
})
export class RolesList implements OnInit {
  private readonly adminUsers = inject(AdminUsersService);

  protected readonly rows = signal<RoleSummary[]>([]);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(1);
  protected readonly totalElements = signal(0);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.adminUsers.listRoles({ page: this.page() }).subscribe({
      next: (result) => {
        this.rows.set(result.content);
        this.totalPages.set(result.page.totalPages);
        this.totalElements.set(result.page.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load roles.');
        this.loading.set(false);
      },
    });
  }

  protected changePage(page: number): void {
    this.page.set(page);
    this.load();
  }
}
