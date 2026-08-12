import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';

import { AdminUser } from '../../../core/models/admin.model';
import { Pagination } from '../../../shared/components/pagination/pagination';
import { AdminUsersService } from '../services/admin-users.service';

type SortField = 'username' | 'email' | 'createdAt';
type ConfirmAction = 'approve' | 'reject';

@Component({
  selector: 'app-admin-pending',
  imports: [Pagination, DatePipe],
  templateUrl: './pending.html',
  styleUrl: '../admin-shared.scss',
})
export class Pending implements OnInit {
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
  protected readonly confirming = signal<{ id: string; action: ConfirmAction } | null>(null);

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.adminUsers
      .listPending({ page: this.page(), sortBy: this.sortBy(), sortDirection: this.sortDirection() })
      .subscribe({
        next: (result) => {
          this.rows.set(result.content);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Could not load pending registrations.');
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

  protected requestConfirm(userId: string, action: ConfirmAction): void {
    this.successMessage.set(null);
    this.confirming.set({ id: userId, action });
  }

  protected cancelConfirm(): void {
    this.confirming.set(null);
  }

  protected confirmApprove(user: AdminUser): void {
    this.pendingActionId.set(user.id);
    this.error.set(null);
    this.adminUsers.approve(user.id).subscribe({
      next: (result) => {
        this.successMessage.set(`Approved ${user.username} — account opened with IBAN ${result.iban}.`);
        this.confirming.set(null);
        this.pendingActionId.set(null);
        this.load();
      },
      error: () => {
        this.error.set(`Could not approve ${user.username}. bankingService may be unreachable — try again shortly.`);
        this.pendingActionId.set(null);
      },
    });
  }

  protected confirmReject(user: AdminUser): void {
    this.pendingActionId.set(user.id);
    this.error.set(null);
    this.adminUsers.reject(user.id).subscribe({
      next: () => {
        this.successMessage.set(`Rejected ${user.username}.`);
        this.confirming.set(null);
        this.pendingActionId.set(null);
        this.load();
      },
      error: () => {
        this.error.set(`Could not reject ${user.username}.`);
        this.pendingActionId.set(null);
      },
    });
  }
}
