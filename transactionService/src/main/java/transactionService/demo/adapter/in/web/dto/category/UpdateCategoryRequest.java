package transactionService.demo.adapter.in.web.dto.category;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(max = 80) String name,
        @Size(max = 255) String description
) {
}
