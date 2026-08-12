package transactionService.demo.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transactionService.demo.domain.exception.DuplicateResourceException;
import transactionService.demo.domain.exception.ResourceInUseException;
import transactionService.demo.domain.exception.ResourceNotFoundException;
import transactionService.demo.domain.model.TransactionCategory;
import transactionService.demo.domain.port.in.CategoryUseCase;
import transactionService.demo.domain.port.out.CategoryRepositoryPort;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CategoryService implements CategoryUseCase {

    private final CategoryRepositoryPort categoryRepositoryPort;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public TransactionCategory createCategory(TransactionCategory category) {
        if (categoryRepositoryPort.existsByName(category.getName())) {
            throw new DuplicateResourceException(
                    "Transaction category with name " + category.getName() + " already exists");
        }
        category.setId(null);
        TransactionCategory saved = categoryRepositoryPort.save(category);
        log.info("Transaction category created with id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public TransactionCategory getCategory(UUID id) {
        return categoryRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction category " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<TransactionCategory> listCategories(Pageable pageable) {
        log.debug("Listing transaction categories with pageable={}", pageable);
        return categoryRepositoryPort.findAll(pageable);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public TransactionCategory updateCategory(UUID id, TransactionCategory updates) {
        TransactionCategory existing = getCategory(id);
        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        TransactionCategory saved = categoryRepositoryPort.save(existing);
        log.info("Transaction category updated with id={}", saved.getId());
        return saved;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCategory(UUID id) {
        if (!categoryRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Transaction category " + id + " not found");
        }
        if (categoryRepositoryPort.isReferenced(id)) {
            throw new ResourceInUseException(
                    "Transaction category " + id + " is referenced by existing transactions or schedules");
        }
        categoryRepositoryPort.deleteById(id);
        log.info("Transaction category deleted with id={}", id);
    }
}
