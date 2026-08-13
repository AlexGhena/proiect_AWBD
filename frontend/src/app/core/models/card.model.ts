export type CardStatus = 'ACTIVE' | 'BLOCKED' | 'EXPIRED' | 'LOST_STOLEN';

// Mirrors bankingService's CardResponse.
export interface Card {
  id: string;
  accountId: string;
  cardReference: string;
  lastFour: string;
  cardholderName: string;
  expiryMonth: number;
  expiryYear: number;
  status: CardStatus;
  createdAt: string;
  updatedAt: string;
}

// Mirrors bankingService's CardDetailsResponse.
export interface CardDetails {
  cardNumber: string;
  cvv: string;
  cardholderName: string;
  expiryMonth: number;
  expiryYear: number;
}
