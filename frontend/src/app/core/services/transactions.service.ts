import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { PageResponse } from '../models/page.model';
import { Transaction } from '../models/transaction.model';

export interface PageQuery {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'asc' | 'desc';
}

const TRANSACTIONS_BASE = '/api/transactions';

function toParams(query: PageQuery): HttpParams {
  let params = new HttpParams();
  if (query.page !== undefined) params = params.set('page', query.page);
  if (query.size !== undefined) params = params.set('size', query.size);
  if (query.sortBy) params = params.set('sortBy', query.sortBy);
  if (query.sortDirection) params = params.set('sortDirection', query.sortDirection);
  return params;
}

@Injectable({ providedIn: 'root' })
export class TransactionsService {
  private readonly http = inject(HttpClient);

  listMine(query: PageQuery = {}): Observable<PageResponse<Transaction>> {
    return this.http.get<PageResponse<Transaction>>(`${TRANSACTIONS_BASE}/me`, { params: toParams(query) });
  }
}
