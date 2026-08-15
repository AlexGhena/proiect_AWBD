import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CategoriesPageResponse } from '../models/category.model';

const CATEGORIES_BASE = '/api/categories';

@Injectable({ providedIn: 'root' })
export class CategoriesService {
  private readonly http = inject(HttpClient);

  list(size = 100): Observable<CategoriesPageResponse> {
    return this.http.get<CategoriesPageResponse>(CATEGORIES_BASE, { params: { size } });
  }
}
