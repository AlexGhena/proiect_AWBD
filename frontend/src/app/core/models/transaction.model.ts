export type TransactionType = 'TRANSFER' | 'DEPOSIT' | 'WITHDRAWAL';

export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'COMPENSATED';

// Mirrors transactionService's TransactionResponse.
export interface Transaction {
  id: string;
  categoryId: string | null;
  scheduledTransactionId: string | null;
  sourceAccountId: string | null;
  destinationAccountId: string | null;
  sagaId: string;
  amount: number;
  currency: string;
  type: TransactionType;
  status: TransactionStatus;
  description: string | null;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}
