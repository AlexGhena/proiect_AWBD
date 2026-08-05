package transactionService.demo.application.service;

import transactionService.demo.domain.exception.DuplicateResourceException;
import transactionService.demo.domain.exception.ResourceInUseException;
import transactionService.demo.domain.exception.ResourceNotFoundException;
import transactionService.demo.domain.model.TransactionCategory;
import transactionService.demo.domain.port.in.CategoryUseCase;
import transactionService.demo.domain.port.out.CategoryRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CategoryService implements CategoryUseCase {

    private final CategoryRepositoryPort categoryRepositoryPort;

    public CategoryService(CategoryRepositoryPort categoryRepositoryPort) {
        this.categoryRepositoryPort = categoryRepositoryPort;
    }

    @Override
    public TransactionCategory createCategory(TransactionCategory category) {
        if (categoryRepositoryPort.existsByName(category.getName())) {
            throw new DuplicateResourceException(
                    "Transaction category with name " + category.getName() + " already exists");
        }
        category.setId(null);
        return categoryRepositoryPort.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionCategory getCategory(UUID id) {
        return categoryRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction category " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionCategory> listCategories(Pageable pageable) {
        return categoryRepositoryPort.findAll(pageable);
    }

    @Override
    public TransactionCategory updateCategory(UUID id, TransactionCategory updates) {
        TransactionCategory existing = getCategory(id);
        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        return categoryRepositoryPort.save(existing);
    }

    @Override
    public void deleteCategory(UUID id) {
        if (!categoryRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Transaction category " + id + " not found");
        }
        if (categoryRepositoryPort.isReferenced(id)) {
            throw new ResourceInUseException(
                    "Transaction category " + id + " is referenced by existing transactions or schedules");
        }
        categoryRepositoryPort.deleteById(id);
    }
}
