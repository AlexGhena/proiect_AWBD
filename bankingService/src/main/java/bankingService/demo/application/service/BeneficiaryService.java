package bankingService.demo.application.service;

import bankingService.demo.domain.exception.DuplicateResourceException;
import bankingService.demo.domain.exception.ResourceNotFoundException;
import bankingService.demo.domain.model.Beneficiary;
import bankingService.demo.domain.port.in.BeneficiaryUseCase;
import bankingService.demo.domain.port.out.AccountRepositoryPort;
import bankingService.demo.domain.port.out.BeneficiaryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class BeneficiaryService implements BeneficiaryUseCase {

    private final BeneficiaryRepositoryPort beneficiaryRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;

    @Override
    public Beneficiary createBeneficiary(Beneficiary beneficiary) {
        if (!accountRepositoryPort.existsById(beneficiary.getOwnerAccountId())) {
            throw new ResourceNotFoundException("Bank account " + beneficiary.getOwnerAccountId() + " not found");
        }
        if (beneficiaryRepositoryPort.existsByOwnerAccountIdAndBeneficiaryIban(
                beneficiary.getOwnerAccountId(), beneficiary.getBeneficiaryIban())) {
            throw new DuplicateResourceException(
                    "Beneficiary with IBAN " + beneficiary.getBeneficiaryIban() + " already saved for this account");
        }
        beneficiary.setId(null);
        return beneficiaryRepositoryPort.save(beneficiary);
    }

    @Override
    @Transactional(readOnly = true)
    public Beneficiary getBeneficiary(UUID id) {
        return beneficiaryRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Beneficiary> listBeneficiaries(Pageable pageable) {
        return beneficiaryRepositoryPort.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Beneficiary> listBeneficiariesByAccount(UUID ownerAccountId) {
        if (!accountRepositoryPort.existsById(ownerAccountId)) {
            throw new ResourceNotFoundException("Bank account " + ownerAccountId + " not found");
        }
        return beneficiaryRepositoryPort.findByOwnerAccountId(ownerAccountId);
    }

    @Override
    public Beneficiary updateBeneficiary(UUID id, Beneficiary updates) {
        Beneficiary existing = getBeneficiary(id);
        if (updates.getBeneficiaryName() != null) {
            existing.setBeneficiaryName(updates.getBeneficiaryName());
        }
        if (updates.getNickname() != null) {
            existing.setNickname(updates.getNickname());
        }
        return beneficiaryRepositoryPort.save(existing);
    }

    @Override
    public void deleteBeneficiary(UUID id) {
        if (!beneficiaryRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Beneficiary " + id + " not found");
        }
        beneficiaryRepositoryPort.deleteById(id);
    }
}
