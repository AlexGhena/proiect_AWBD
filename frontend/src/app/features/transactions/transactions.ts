import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { Account } from '../../core/models/account.model';
import { Category } from '../../core/models/category.model';
import { Transaction, TransferRequest } from '../../core/models/transaction.model';
import { AccountsService } from '../../core/services/accounts.service';
import { CategoriesService } from '../../core/services/categories.service';
import { TransactionSortField, TransactionsService } from '../../core/services/transactions.service';
import { Topbar } from '../../layout/topbar/topbar';
import { Pagination } from '../../shared/components/pagination/pagination';

type DestinationMode = 'own' | 'external';
type FormStep = 'form' | 'confirm';

@Component({
  selector: 'app-transactions',
  imports: [Topbar, Pagination, CurrencyPipe, DatePipe],
  templateUrl: './transactions.html',
  styleUrl: './transactions.scss',
})
export class Transactions implements OnInit {
  private readonly accountsService = inject(AccountsService);
  private readonly categoriesService = inject(CategoriesService);
  private readonly transactionsService = inject(TransactionsService);

  protected readonly rows = signal<Transaction[]>([]);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(1);
  protected readonly totalElements = signal(0);
  protected readonly sortBy = signal<TransactionSortField>('createdAt');
  protected readonly sortDirection = signal<'asc' | 'desc'>('desc');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly accounts = signal<Account[]>([]);
  protected readonly categories = signal<Category[]>([]);

  protected readonly formOpen = signal(false);
  protected readonly step = signal<FormStep>('form');
  protected readonly sourceAccountId = signal('');
  protected readonly destinationMode = signal<DestinationMode>('own');
  protected readonly destinationAccountId = signal('');
  protected readonly destinationExternalId = signal('');
  protected readonly amountInput = signal('');
  protected readonly descriptionInput = signal('');
  protected readonly categoryId = signal('');
  protected readonly formError = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly successMessage = signal<string | null>(null);

  private readonly myAccountIds = computed(() => new Set(this.accounts().map((account) => account.id)));

  protected readonly activeAccounts = computed(() => this.accounts().filter((a) => a.status === 'ACTIVE'));
  protected readonly eligibleDestinations = computed(() =>
    this.activeAccounts().filter((a) => a.id !== this.sourceAccountId()),
  );
  protected readonly sourceAccount = computed(
    () => this.accounts().find((a) => a.id === this.sourceAccountId()) ?? null,
  );
  protected readonly destinationAccountIdResolved = computed(() =>
    this.destinationMode() === 'own' ? this.destinationAccountId() : this.destinationExternalId().trim(),
  );
  protected readonly destinationAccount = computed(
    () => this.accounts().find((a) => a.id === this.destinationAccountIdResolved()) ?? null,
  );
  protected readonly selectedCategory = computed(
    () => this.categories().find((c) => c.id === this.categoryId()) ?? null,
  );

  ngOnInit(): void {
    this.loadAccounts();
    this.loadCategories();
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

  protected sortByField(field: TransactionSortField): void {
    if (this.sortBy() === field) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortBy.set(field);
      this.sortDirection.set('asc');
    }
    this.page.set(0);
    this.loadTransactions();
  }

  protected changePage(page: number): void {
    this.page.set(page);
    this.loadTransactions();
  }

  protected openForm(): void {
    this.formOpen.set(true);
    this.step.set('form');
    this.formError.set(null);
    this.successMessage.set(null);
    this.sourceAccountId.set(this.activeAccounts()[0]?.id ?? '');
    this.destinationMode.set('own');
    this.destinationAccountId.set('');
    this.destinationExternalId.set('');
    this.amountInput.set('');
    this.descriptionInput.set('');
    this.categoryId.set('');
  }

  protected closeForm(): void {
    this.formOpen.set(false);
    this.step.set('form');
    this.formError.set(null);
  }

  protected setDestinationMode(mode: DestinationMode): void {
    this.destinationMode.set(mode);
    this.destinationAccountId.set('');
    this.destinationExternalId.set('');
  }

  protected reviewTransfer(): void {
    this.formError.set(null);

    const source = this.sourceAccount();
    if (!source) {
      this.formError.set('Select a source account.');
      return;
    }

    const destinationId = this.destinationAccountIdResolved();
    if (!destinationId) {
      this.formError.set(
        this.destinationMode() === 'own' ? 'Select a destination account.' : 'Enter a destination account ID.',
      );
      return;
    }
    if (destinationId === source.id) {
      this.formError.set('Source and destination accounts must be different.');
      return;
    }

    const amount = Number(this.amountInput());
    if (!Number.isFinite(amount) || amount <= 0) {
      this.formError.set('Enter a valid amount greater than zero.');
      return;
    }

    this.step.set('confirm');
  }

  protected backToForm(): void {
    this.step.set('form');
    this.formError.set(null);
  }

  protected submitTransfer(): void {
    const source = this.sourceAccount();
    const destinationId = this.destinationAccountIdResolved();
    if (!source || !destinationId) {
      return;
    }

    const request: TransferRequest = {
      sourceAccountId: source.id,
      destinationAccountId: destinationId,
      amount: Number(this.amountInput()),
      currency: source.currency,
      description: this.descriptionInput().trim() || undefined,
      categoryId: this.categoryId() || undefined,
    };

    this.submitting.set(true);
    this.formError.set(null);
    this.transactionsService.transfer(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.formOpen.set(false);
        this.successMessage.set('Transfer completed successfully.');
        this.loadAccounts();
        this.page.set(0);
        this.loadTransactions();
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.step.set('form');
        this.formError.set(this.describeTransferError(err));
      },
    });
  }

  private describeTransferError(err: HttpErrorResponse): string {
    const detail =
      err.error && typeof err.error === 'object' && 'detail' in err.error
        ? String((err.error as { detail: unknown }).detail)
        : null;

    switch (err.status) {
      case 400:
        return detail ?? 'Check the transfer details and try again.';
      case 401:
      case 403:
        return 'You are not authorized to transfer from this account.';
      case 404:
        return detail ?? 'The selected category could not be found.';
      case 409:
        return detail ?? 'This transfer was rejected — check that both accounts are active and use the same currency.';
      case 503:
        return 'The transfer could not be completed right now. Please try again shortly.';
      default:
        return 'Something went wrong while processing the transfer.';
    }
  }

  private loadAccounts(): void {
    this.accountsService.listMine({ size: 100 }).subscribe({
      next: (result) => this.accounts.set(result.content),
      error: () => this.accounts.set([]),
    });
  }

  private loadCategories(): void {
    this.categoriesService.list(100).subscribe({
      next: (result) => this.categories.set(result.content),
      error: () => this.categories.set([]),
    });
  }

  private loadTransactions(): void {
    this.loading.set(true);
    this.error.set(null);
    this.transactionsService
      .listMine({ page: this.page(), sortBy: this.sortBy(), sortDirection: this.sortDirection() })
      .subscribe({
        next: (result) => {
          this.rows.set(result.content);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Could not load your transactions.');
          this.loading.set(false);
        },
      });
  }
}
