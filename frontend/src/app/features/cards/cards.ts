import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

import { Account } from '../../core/models/account.model';
import { Card, CardDetails } from '../../core/models/card.model';
import { AccountsService } from '../../core/services/accounts.service';
import { CardsService } from '../../core/services/cards.service';
import { Topbar } from '../../layout/topbar/topbar';

interface CardRow extends Card {
  accountIban: string;
}

type CardAction = 'reveal-details' | 'reveal-pin' | 'change-pin' | 'report-lost' | 'block';

@Component({
  selector: 'app-cards',
  imports: [Topbar],
  templateUrl: './cards.html',
  styleUrl: './cards.scss',
})
export class Cards implements OnInit {
  private readonly accountsService = inject(AccountsService);
  private readonly cardsService = inject(CardsService);

  protected readonly rows = signal<CardRow[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);

  protected readonly activeAction = signal<{ cardId: string; action: CardAction } | null>(null);
  protected readonly actionBusy = signal(false);
  protected readonly actionError = signal<string | null>(null);

  protected readonly passwordInput = signal('');
  protected readonly newPinInput = signal('');
  protected readonly confirmPinInput = signal('');

  protected readonly revealedDetails = signal<Record<string, CardDetails>>({});
  protected readonly revealedPins = signal<Record<string, string>>({});

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.accountsService
      .listMine({ size: 100 })
      .pipe(
        switchMap((accountsPage) => {
          const accounts = accountsPage.content;
          if (accounts.length === 0) {
            return of<CardRow[]>([]);
          }
          const perAccount = accounts.map((account) =>
            this.cardsService.listByAccount(account.id).pipe(
              switchMap((cards) => of(cards.map((card) => this.toRow(card, account)))),
              catchError(() => of<CardRow[]>([])),
            ),
          );
          return forkJoin(perAccount).pipe(switchMap((lists) => of(lists.flat())));
        }),
      )
      .subscribe({
        next: (rows) => {
          this.rows.set(rows);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Could not load your cards.');
          this.loading.set(false);
        },
      });
  }

  private toRow(card: Card, account: Account): CardRow {
    return { ...card, accountIban: account.iban };
  }

  protected canManage(card: CardRow): boolean {
    return card.status === 'ACTIVE' || card.status === 'BLOCKED';
  }

  protected isActionOpen(cardId: string, action: CardAction): boolean {
    const active = this.activeAction();
    return active !== null && active.cardId === cardId && active.action === action;
  }

  protected openAction(cardId: string, action: CardAction): void {
    this.successMessage.set(null);
    this.actionError.set(null);
    this.passwordInput.set('');
    this.newPinInput.set('');
    this.confirmPinInput.set('');
    this.activeAction.set({ cardId, action });
  }

  protected cancelAction(): void {
    this.activeAction.set(null);
    this.actionError.set(null);
  }

  protected submitRevealDetails(card: CardRow): void {
    const password = this.passwordInput();
    if (!password) {
      this.actionError.set('Enter your password.');
      return;
    }
    this.actionBusy.set(true);
    this.actionError.set(null);
    this.cardsService.revealDetails(card.id, password).subscribe({
      next: (details) => {
        this.revealedDetails.update((map) => ({ ...map, [card.id]: details }));
        this.actionBusy.set(false);
        this.activeAction.set(null);
      },
      error: (err: HttpErrorResponse) => {
        this.actionBusy.set(false);
        this.actionError.set(err.status === 403 ? 'Incorrect password.' : 'Could not load card details.');
      },
    });
  }

  protected submitRevealPin(card: CardRow): void {
    const password = this.passwordInput();
    if (!password) {
      this.actionError.set('Enter your password.');
      return;
    }
    this.actionBusy.set(true);
    this.actionError.set(null);
    this.cardsService.revealPin(card.id, password).subscribe({
      next: (result) => {
        this.revealedPins.update((map) => ({ ...map, [card.id]: result.pin }));
        this.actionBusy.set(false);
        this.activeAction.set(null);
      },
      error: (err: HttpErrorResponse) => {
        this.actionBusy.set(false);
        this.actionError.set(err.status === 403 ? 'Incorrect password.' : 'Could not load PIN.');
      },
    });
  }

  protected submitChangePin(card: CardRow): void {
    const password = this.passwordInput();
    const newPin = this.newPinInput();
    const confirmPin = this.confirmPinInput();
    if (!password) {
      this.actionError.set('Enter your current password.');
      return;
    }
    if (!/^[0-9]{4}$/.test(newPin)) {
      this.actionError.set('New PIN must be exactly 4 digits.');
      return;
    }
    if (newPin !== confirmPin) {
      this.actionError.set('PIN confirmation does not match.');
      return;
    }
    this.actionBusy.set(true);
    this.actionError.set(null);
    this.cardsService.changePin(card.id, password, newPin).subscribe({
      next: () => {
        this.actionBusy.set(false);
        this.activeAction.set(null);
        this.clearRevealedPin(card.id);
        this.successMessage.set(`PIN changed for card ending in ${card.lastFour}.`);
      },
      error: (err: HttpErrorResponse) => {
        this.actionBusy.set(false);
        this.actionError.set(err.status === 403 ? 'Incorrect password.' : 'Could not change the PIN.');
      },
    });
  }

  protected submitReportLost(card: CardRow): void {
    this.actionBusy.set(true);
    this.actionError.set(null);
    this.cardsService.reportLost(card.id).subscribe({
      next: (updated) => {
        this.rows.update((rows) => rows.map((row) => (row.id === card.id ? { ...row, ...updated } : row)));
        this.clearRevealedDetails(card.id);
        this.clearRevealedPin(card.id);
        this.actionBusy.set(false);
        this.activeAction.set(null);
        this.successMessage.set(`Card ending in ${card.lastFour} reported lost/stolen.`);
      },
      error: () => {
        this.actionBusy.set(false);
        this.actionError.set(`Could not report card ending in ${card.lastFour}.`);
      },
    });
  }

  protected submitToggleStatus(card: CardRow): void {
    const nextStatus = card.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE';
    this.actionBusy.set(true);
    this.actionError.set(null);
    this.cardsService.updateStatus(card.id, nextStatus).subscribe({
      next: (updated) => {
        this.rows.update((rows) => rows.map((row) => (row.id === card.id ? { ...row, ...updated } : row)));
        this.actionBusy.set(false);
        this.activeAction.set(null);
        this.successMessage.set(
          nextStatus === 'BLOCKED'
            ? `Card ending in ${card.lastFour} blocked.`
            : `Card ending in ${card.lastFour} unblocked.`,
        );
      },
      error: () => {
        this.actionBusy.set(false);
        this.actionError.set(`Could not update card ending in ${card.lastFour}.`);
      },
    });
  }

  protected formatCardNumber(cardNumber: string): string {
    return cardNumber.replace(/(.{4})/g, '$1 ').trim();
  }

  private clearRevealedDetails(cardId: string): void {
    this.revealedDetails.update((map) => {
      const { [cardId]: _removed, ...rest } = map;
      return rest;
    });
  }

  private clearRevealedPin(cardId: string): void {
    this.revealedPins.update((map) => {
      const { [cardId]: _removed, ...rest } = map;
      return rest;
    });
  }
}
