import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';

import { AdminUser } from '../../../core/models/admin.model';
import { Pagination } from '../../../shared/components/pagination/pagination';
import { AdminUsersService } from '../services/admin-users.service';

type SortField = 'username' | 'email' | 'createdAt';

@Component({
  selector: 'app-admin-users-list',
  imports: [Pagination, DatePipe],
  templateUrl: './users-list.html',
  styleUrl: '../admin-shared.scss',
})
export class UsersList implements OnInit {
  private readonly adminUsers = inject(AdminUsersService);

  protected readonly rows = signal<AdminUser[]>([]);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(1);
  protected readonly totalElements = signal(0);
  protected readonly sortBy = signal<SortField>('createdAt');
  protected readonly sortDirection = signal<'asc' | 'desc'>('desc');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly pendingActionId = signal<string | null>(null);
  protected readonly confirmingId = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.adminUsers
      .listUsers({ page: this.page(), sortBy: this.sortBy(), sortDirection: this.sortDirection() })
      .subscribe({
        next: (result) => {
          this.rows.set(result.content);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Could not load users.');
          this.loading.set(false);
        },
      });
  }

  protected sortByField(field: SortField): void {
    if (this.sortBy() === field) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortBy.set(field);
      this.sortDirection.set('asc');
    }
    this.page.set(0);
    this.load();
  }

  protected changePage(page: number): void {
    this.page.set(page);
    this.load();
  }

  protected requestDeactivate(userId: string): void {
    this.successMessage.set(null);
    this.confirmingId.set(userId);
  }

  protected cancelConfirm(): void {
    this.confirmingId.set(null);
  }

  protected confirmDeactivate(user: AdminUser): void {
    this.pendingActionId.set(user.id);
    this.error.set(null);
    this.adminUsers.deactivate(user.id).subscribe({
      next: () => {
        this.successMessage.set(`Deactivated ${user.username}.`);
        this.confirmingId.set(null);
        this.pendingActionId.set(null);
        this.load();
      },
      error: () => {
        this.error.set(`Could not deactivate ${user.username}.`);
        this.pendingActionId.set(null);
      },
    });
  }
}
