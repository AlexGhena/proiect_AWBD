package bankingService.demo.domain.port.out;

import bankingService.demo.domain.model.Beneficiary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BeneficiaryRepositoryPort {

    Beneficiary save(Beneficiary beneficiary);

    Optional<Beneficiary> findById(UUID id);

    boolean existsById(UUID id);

    boolean existsByOwnerAccountIdAndBeneficiaryIban(UUID ownerAccountId, String beneficiaryIban);

    Page<Beneficiary> findAll(Pageable pageable);

    List<Beneficiary> findByOwnerAccountId(UUID ownerAccountId);

    void deleteById(UUID id);
}
