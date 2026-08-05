package transactionService.demo.domain.port.out;

import transactionService.demo.domain.model.TransactionCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepositoryPort {

    TransactionCategory save(TransactionCategory category);

    Optional<TransactionCategory> findById(UUID id);

    boolean existsById(UUID id);

    boolean existsByName(String name);

    boolean isReferenced(UUID categoryId);

    Page<TransactionCategory> findAll(Pageable pageable);

    void deleteById(UUID id);
}
