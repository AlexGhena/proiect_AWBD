// The flat pagination envelope shared by bankingService and transactionService list endpoints.
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  sortBy: string;
  sortDirection: string;
}
