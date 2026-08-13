import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';

import { Account } from '../../core/models/account.model';
import { AccountsService } from '../../core/services/accounts.service';
import { Topbar } from '../../layout/topbar/topbar';
import { Pagination } from '../../shared/components/pagination/pagination';

type SortField = 'iban' | 'balance' | 'createdAt';

@Component({
  selector: 'app-accounts',
  imports: [Topbar, Pagination, CurrencyPipe, DatePipe],
  templateUrl: './accounts.html',
  styleUrl: './accounts.scss',
})
export class Accounts implements OnInit {
  private readonly accountsService = inject(AccountsService);

  protected readonly rows = signal<Account[]>([]);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(1);
  protected readonly totalElements = signal(0);
  protected readonly sortBy = signal<SortField>('createdAt');
  protected readonly sortDirection = signal<'asc' | 'desc'>('desc');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
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

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.accountsService
      .listMine({ page: this.page(), sortBy: this.sortBy(), sortDirection: this.sortDirection() })
      .subscribe({
        next: (result) => {
          this.rows.set(result.content);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Could not load your accounts.');
          this.loading.set(false);
        },
      });
  }
}
