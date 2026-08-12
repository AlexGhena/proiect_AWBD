import { Injectable, signal } from '@angular/core';

// In-memory only, never localStorage — refresh re-mints via POST /api/auth/token.
@Injectable({ providedIn: 'root' })
export class TokenStore {
  private readonly token = signal<string | null>(null);

  readonly accessToken = this.token.asReadonly();

  setAccessToken(token: string): void {
    this.token.set(token);
  }

  clear(): void {
    this.token.set(null);
  }
}
