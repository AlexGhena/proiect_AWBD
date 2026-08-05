package transactionService.demo.adapter.in.web.mapper;

import transactionService.demo.adapter.in.web.dto.category.CategoryResponse;
import transactionService.demo.adapter.in.web.dto.category.CreateCategoryRequest;
import transactionService.demo.adapter.in.web.dto.category.UpdateCategoryRequest;
import transactionService.demo.domain.model.TransactionCategory;
import org.springframework.stereotype.Component;

@Component
public class CategoryWebMapper {

    public TransactionCategory toDomain(CreateCategoryRequest request) {
        return TransactionCategory.builder()
                .name(request.name())
                .description(request.description())
                .build();
    }

    public TransactionCategory toDomain(UpdateCategoryRequest request) {
        return TransactionCategory.builder()
                .name(request.name())
                .description(request.description())
                .build();
    }

    public CategoryResponse toResponse(TransactionCategory domain) {
        return new CategoryResponse(
                domain.getId(),
                domain.getName(),
                domain.getDescription(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
