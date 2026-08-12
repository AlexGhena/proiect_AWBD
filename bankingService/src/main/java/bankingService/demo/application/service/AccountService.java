package bankingService.demo.application.service;

import bankingService.demo.domain.exception.DuplicateResourceException;
import bankingService.demo.domain.exception.ResourceNotFoundException;
import bankingService.demo.domain.model.AccountStatus;
import bankingService.demo.domain.model.BankAccount;
import bankingService.demo.domain.port.in.AccountUseCase;
import bankingService.demo.domain.port.out.AccountRepositoryPort;
import bankingService.demo.security.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AccountService implements AccountUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final UserServiceClient userServiceClient;

    @Override
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.isSelf(#account.userId)")
    public BankAccount createAccount(BankAccount account) {
        if (!userServiceClient.userExists(account.getUserId())) {
            throw new ResourceNotFoundException("User " + account.getUserId() + " not found");
        }
        if (accountRepositoryPort.existsByIban(account.getIban())) {
            throw new DuplicateResourceException(
                    "Bank account with IBAN " + account.getIban() + " already exists");
        }
        account.setId(null);
        account.setVersion(null);
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        if (account.getStatus() == null) {
            account.setStatus(AccountStatus.ACTIVE);
        }
        BankAccount saved = accountRepositoryPort.save(account);
        log.info("Bank account created with id={}, userId={}", saved.getId(), saved.getUserId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.ownsAccount(#id)")
    public BankAccount getAccount(UUID id) {
        return accountRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<BankAccount> listAccounts(Pageable pageable) {
        log.debug("Listing bank accounts with pageable={}", pageable);
        return accountRepositoryPort.findAll(pageable);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.ownsAccount(#id)")
    public BankAccount updateAccount(UUID id, BankAccount updates) {
        BankAccount existing = getAccount(id);
        if (updates.getCurrency() != null) {
            existing.setCurrency(updates.getCurrency());
        }
        if (updates.getBalance() != null) {
            existing.setBalance(updates.getBalance());
        }
        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }
        BankAccount saved = accountRepositoryPort.save(existing);
        log.info("Bank account updated with id={}", saved.getId());
        return saved;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.ownsAccount(#id)")
    public void deleteAccount(UUID id) {
        if (!accountRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Bank account " + id + " not found");
        }
        accountRepositoryPort.deleteById(id);
        log.info("Bank account deleted with id={}", id);
    }
}
