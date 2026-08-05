package transactionService.demo.domain.port.in;

import transactionService.demo.domain.model.TransactionCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CategoryUseCase {

    TransactionCategory createCategory(TransactionCategory category);

    TransactionCategory getCategory(UUID id);

    Page<TransactionCategory> listCategories(Pageable pageable);

    TransactionCategory updateCategory(UUID id, TransactionCategory updates);

    void deleteCategory(UUID id);
}
