export type AccountStatus = 'ACTIVE' | 'BLOCKED' | 'CLOSED';

// Mirrors bankingService's AccountLookupResponse: the minimal, non-sensitive view returned when
// resolving an IBAN to pick a transfer destination (no balance or owner).
export interface AccountLookup {
  id: string;
  iban: string;
  currency: string;
  status: AccountStatus;
}

// Mirrors bankingService's AccountResponse.
export interface Account {
  id: string;
  userId: string;
  iban: string;
  currency: string;
  balance: number;
  status: AccountStatus;
  version: number;
  createdAt: string;
  updatedAt: string;
}
