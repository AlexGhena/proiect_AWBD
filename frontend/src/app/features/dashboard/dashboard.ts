import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { Account } from '../../core/models/account.model';
import { Transaction } from '../../core/models/transaction.model';
import { AccountsService } from '../../core/services/accounts.service';
import { TransactionsService } from '../../core/services/transactions.service';
import { Topbar } from '../../layout/topbar/topbar';

const DASHBOARD_PAGE_SIZE = 5;

@Component({
  selector: 'app-dashboard',
  imports: [Topbar, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly accountsService = inject(AccountsService);
  private readonly transactionsService = inject(TransactionsService);

  protected readonly currentUser = this.authService.currentUser;

  protected readonly accounts = signal<Account[]>([]);
  protected readonly accountsTotal = signal(0);
  protected readonly accountsLoading = signal(true);
  protected readonly accountsError = signal<string | null>(null);

  protected readonly transactions = signal<Transaction[]>([]);
  protected readonly transactionsTotal = signal(0);
  protected readonly transactionsLoading = signal(true);
  protected readonly transactionsError = signal<string | null>(null);

  private readonly myAccountIds = computed(() => new Set(this.accounts().map((account) => account.id)));

  ngOnInit(): void {
    this.loadAccounts();
    this.loadTransactions();
  }

  protected direction(transaction: Transaction): 'credit' | 'debit' | 'neutral' {
    if (transaction.status !== 'COMPLETED') {
      return 'neutral';
    }
    const mine = this.myAccountIds();
    const isSource = transaction.sourceAccountId !== null && mine.has(transaction.sourceAccountId);
    const isDestination =
      transaction.destinationAccountId !== null && mine.has(transaction.destinationAccountId);
    if (isSource === isDestination) {
      return 'neutral';
    }
    return isSource ? 'debit' : 'credit';
  }

  private loadAccounts(): void {
    this.accountsService
      .listMine({ size: DASHBOARD_PAGE_SIZE, sortBy: 'createdAt', sortDirection: 'desc' })
      .subscribe({
        next: (result) => {
          this.accounts.set(result.content);
          this.accountsTotal.set(result.totalElements);
          this.accountsLoading.set(false);
        },
        error: () => {
          this.accountsError.set('Could not load your accounts.');
          this.accountsLoading.set(false);
        },
      });
  }

  private loadTransactions(): void {
    this.transactionsService
      .listMine({ size: DASHBOARD_PAGE_SIZE, sortBy: 'createdAt', sortDirection: 'desc' })
      .subscribe({
        next: (result) => {
          this.transactions.set(result.content);
          this.transactionsTotal.set(result.totalElements);
          this.transactionsLoading.set(false);
        },
        error: () => {
          this.transactionsError.set('Could not load recent activity.');
          this.transactionsLoading.set(false);
        },
      });
  }
}
