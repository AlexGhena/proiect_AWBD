package transactionService.demo.adapter.in.web;

import transactionService.demo.adapter.in.web.dto.category.CategoryResponse;
import transactionService.demo.adapter.in.web.dto.category.CreateCategoryRequest;
import transactionService.demo.adapter.in.web.dto.category.UpdateCategoryRequest;
import transactionService.demo.adapter.in.web.mapper.CategoryWebMapper;
import transactionService.demo.domain.model.TransactionCategory;
import transactionService.demo.domain.port.in.CategoryUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryUseCase categoryUseCase;
    private final CategoryWebMapper mapper;

    public CategoryController(CategoryUseCase categoryUseCase, CategoryWebMapper mapper) {
        this.categoryUseCase = categoryUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        TransactionCategory created = categoryUseCase.createCategory(mapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(categoryUseCase.getCategory(id)));
    }

    @GetMapping
    public ResponseEntity<PagedModel<CategoryResponse>> list(Pageable pageable) {
        Page<CategoryResponse> page = categoryUseCase.listCategories(pageable).map(mapper::toResponse);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdateCategoryRequest request) {
        TransactionCategory updated = categoryUseCase.updateCategory(id, mapper.toDomain(request));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryUseCase.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
