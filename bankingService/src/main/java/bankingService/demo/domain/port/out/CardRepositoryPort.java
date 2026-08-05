package bankingService.demo.domain.port.out;

import bankingService.demo.domain.model.BankCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepositoryPort {

    BankCard save(BankCard card);

    Optional<BankCard> findById(UUID id);

    boolean existsById(UUID id);

    boolean existsByCardReference(String cardReference);

    Page<BankCard> findAll(Pageable pageable);

    List<BankCard> findByAccountId(UUID accountId);

    void deleteById(UUID id);
}
