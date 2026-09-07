// Mirrors transactionService's CategoryResponse.
export interface Category {
  id: string;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

// /api/categories uses Spring Data's PagedModel, with page metadata nested differently.
export interface CategoriesPageResponse {
  content: Category[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
