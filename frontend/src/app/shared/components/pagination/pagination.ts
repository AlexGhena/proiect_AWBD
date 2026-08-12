import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.html',
  styleUrl: './pagination.scss',
})
export class Pagination {
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly totalElements = input<number>();

  readonly pageChange = output<number>();

  protected readonly isFirst = computed(() => this.page() <= 0);
  protected readonly isLast = computed(() => this.page() >= this.totalPages() - 1);
  protected readonly displayPage = computed(() => this.page() + 1);
  protected readonly displayTotalPages = computed(() => Math.max(this.totalPages(), 1));

  protected previous(): void {
    if (!this.isFirst()) {
      this.pageChange.emit(this.page() - 1);
    }
  }

  protected next(): void {
    if (!this.isLast()) {
      this.pageChange.emit(this.page() + 1);
    }
  }
}
