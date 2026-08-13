import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Card, CardDetails, CardStatus } from '../models/card.model';

const CARDS_BASE = '/api/cards';

@Injectable({ providedIn: 'root' })
export class CardsService {
  private readonly http = inject(HttpClient);

  listByAccount(accountId: string): Observable<Card[]> {
    return this.http.get<Card[]>(`/api/accounts/${accountId}/cards`);
  }

  updateStatus(cardId: string, status: CardStatus): Observable<Card> {
    return this.http.put<Card>(`${CARDS_BASE}/${cardId}`, { status });
  }

  revealDetails(cardId: string, password: string): Observable<CardDetails> {
    return this.http.post<CardDetails>(`${CARDS_BASE}/${cardId}/reveal`, { password });
  }

  revealPin(cardId: string, password: string): Observable<{ pin: string }> {
    return this.http.post<{ pin: string }>(`${CARDS_BASE}/${cardId}/pin/reveal`, { password });
  }

  changePin(cardId: string, currentPassword: string, newPin: string): Observable<void> {
    return this.http.put<void>(`${CARDS_BASE}/${cardId}/pin`, { currentPassword, newPin });
  }

  reportLost(cardId: string): Observable<Card> {
    return this.http.post<Card>(`${CARDS_BASE}/${cardId}/report-lost`, {});
  }
}
