package bankingService.demo.domain.port.in;

import bankingService.demo.domain.model.Beneficiary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BeneficiaryUseCase {

    Beneficiary createBeneficiary(Beneficiary beneficiary);

    Beneficiary getBeneficiary(UUID id);

    Page<Beneficiary> listBeneficiaries(Pageable pageable);

    List<Beneficiary> listBeneficiariesByAccount(UUID ownerAccountId);

    Beneficiary updateBeneficiary(UUID id, Beneficiary updates);

    void deleteBeneficiary(UUID id);
}
