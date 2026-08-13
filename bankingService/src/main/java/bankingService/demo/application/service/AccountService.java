package bankingService.demo.application.service;

import bankingService.demo.adapter.out.idempotency.IdempotencyKeyJpaEntity;
import bankingService.demo.adapter.out.idempotency.IdempotencyStore;
import bankingService.demo.domain.exception.DuplicateResourceException;
import bankingService.demo.domain.exception.InsufficientFundsException;
import bankingService.demo.domain.exception.InvalidAccountStateException;
import bankingService.demo.domain.exception.ResourceNotFoundException;
import bankingService.demo.domain.model.AccountOperationResult;
import bankingService.demo.domain.model.AccountSnapshot;
import bankingService.demo.domain.model.AccountStatus;
import bankingService.demo.domain.model.BankAccount;
import bankingService.demo.domain.model.BankCard;
import bankingService.demo.domain.model.CardStatus;
import bankingService.demo.domain.model.IdempotentOperationType;
import bankingService.demo.domain.port.in.AccountTransferUseCase;
import bankingService.demo.domain.port.in.AccountUseCase;
import bankingService.demo.domain.port.out.AccountRepositoryPort;
import bankingService.demo.domain.port.out.CardRepositoryPort;
import bankingService.demo.security.AuthenticatedUser;
import bankingService.demo.security.AuthenticatedUserResolver;
import bankingService.demo.security.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AccountService implements AccountUseCase, AccountTransferUseCase {

    private static final int MAX_OPTIMISTIC_LOCK_ATTEMPTS = 3;
    private static final int CARD_VALIDITY_YEARS = 4;
    private static final String FALLBACK_CARDHOLDER_NAME = "Card Holder";

    private final AccountRepositoryPort accountRepositoryPort;
    private final UserServiceClient userServiceClient;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final IdempotencyStore idempotencyStore;
    private final IbanGenerator ibanGenerator;
    private final CardRepositoryPort cardRepositoryPort;
    private final CardIssuanceGenerator cardIssuanceGenerator;

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
        issueDebitCard(saved);
        return saved;
    }

    @Override
    public BankAccount provisionAccount(UUID userId, String currency) {
        String iban = ibanGenerator.generate();
        BankAccount account = BankAccount.builder()
                .userId(userId)
                .iban(iban)
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();
        BankAccount saved = accountRepositoryPort.save(account);
        log.info("Provisioned bank account id={}, userId={}, iban={}", saved.getId(), userId, iban);
        issueDebitCard(saved);
        return saved;
    }

    /**
     * Every newly opened account gets a debit card automatically. Saved directly through the
     * repository port rather than {@code CardUseCase.createCard}, since that use case is gated by
     * {@code @accountSecurity.ownsAccount} - a check tied to the *caller's* identity that doesn't
     * apply here (e.g. {@link #provisionAccount} is reachable by any authenticated internal caller,
     * not just the account's own owner). The cardholder name is a best-effort lookup against
     * userService - a missing/unreachable profile falls back to a placeholder rather than blocking
     * account creation.
     */
    private void issueDebitCard(BankAccount account) {
        String cardholderName = userServiceClient.getCardholderName(account.getUserId())
                .orElse(FALLBACK_CARDHOLDER_NAME);
        LocalDate expiry = LocalDate.now().plusYears(CARD_VALIDITY_YEARS);
        String lastFour = cardIssuanceGenerator.lastFour();
        BankCard card = BankCard.builder()
                .accountId(account.getId())
                .cardReference(cardIssuanceGenerator.cardReference())
                .lastFour(lastFour)
                .cardNumber(cardIssuanceGenerator.cardNumberEndingIn(lastFour))
                .cvv(cardIssuanceGenerator.cvv())
                .pin(cardIssuanceGenerator.pin())
                .cardholderName(cardholderName)
                .expiryMonth(expiry.getMonthValue())
                .expiryYear(expiry.getYear())
                .status(CardStatus.ACTIVE)
                .build();
        BankCard savedCard = cardRepositoryPort.save(card);
        log.info("Issued debit card id={} for accountId={}", savedCard.getId(), account.getId());
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
    @Transactional(readOnly = true)
    public Page<BankAccount> listMyAccounts(Pageable pageable) {
        UUID callerId = authenticatedUserResolver.current()
                .map(AuthenticatedUser::id)
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));
        return accountRepositoryPort.findByUserId(callerId, pageable);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.ownsAccount(#id)")
    public BankAccount updateAccount(UUID id, BankAccount updates) {
        BankAccount existing = getAccount(id);
        if (updates.getCurrency() != null) {
            existing.setCurrency(updates.getCurrency());
        }
        if (updates.getBalance() != null) {
            boolean isAdmin = authenticatedUserResolver.current()
                    .map(AuthenticatedUser::isAdmin)
                    .orElse(false);
            if (!isAdmin) {
                throw new AccessDeniedException("Only an administrator may modify account balance directly");
            }
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

    @Override
    @Transactional(readOnly = true)
    public AccountSnapshot getSnapshot(UUID accountId) {
        BankAccount account = accountRepositoryPort.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account " + accountId + " not found"));
        return AccountSnapshot.builder()
                .id(account.getId())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .build();
    }

    @Override
    public AccountOperationResult debit(UUID accountId, BigDecimal amount, String currency, UUID sagaId,
                                         String idempotencyKey) {
        Optional<IdempotencyKeyJpaEntity> existing = idempotencyStore.find(idempotencyKey);
        if (existing.isPresent()) {
            idempotencyStore.verifyReplayMatches(existing.get(), accountId, IdempotentOperationType.DEBIT, amount);
            log.info("Idempotent replay of debit: key={}, accountId={}, sagaId={}", idempotencyKey, accountId, sagaId);
            return result(existing.get(), true);
        }
        BankAccount updated = applyWithOptimisticRetry(accountId, currency, account -> {
            if (account.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(
                        "Account " + accountId + " has insufficient funds for a debit of " + amount);
            }
            account.setBalance(account.getBalance().subtract(amount));
        });
        idempotencyStore.record(idempotencyKey, accountId, IdempotentOperationType.DEBIT, sagaId, amount, currency,
                updated.getBalance());
        log.info("Debited accountId={}, amount={}, sagaId={}, idempotencyKey={}, newBalance={}",
                accountId, amount, sagaId, idempotencyKey, updated.getBalance());
        return AccountOperationResult.builder().accountId(accountId).balance(updated.getBalance()).replayed(false).build();
    }

    @Override
    public AccountOperationResult credit(UUID accountId, BigDecimal amount, String currency, UUID sagaId,
                                          String idempotencyKey) {
        Optional<IdempotencyKeyJpaEntity> existing = idempotencyStore.find(idempotencyKey);
        if (existing.isPresent()) {
            idempotencyStore.verifyReplayMatches(existing.get(), accountId, IdempotentOperationType.CREDIT, amount);
            log.info("Idempotent replay of credit: key={}, accountId={}, sagaId={}", idempotencyKey, accountId, sagaId);
            return result(existing.get(), true);
        }
        BankAccount updated = applyWithOptimisticRetry(accountId, currency,
                account -> account.setBalance(account.getBalance().add(amount)));
        idempotencyStore.record(idempotencyKey, accountId, IdempotentOperationType.CREDIT, sagaId, amount, currency,
                updated.getBalance());
        log.info("Credited accountId={}, amount={}, sagaId={}, idempotencyKey={}, newBalance={}",
                accountId, amount, sagaId, idempotencyKey, updated.getBalance());
        return AccountOperationResult.builder().accountId(accountId).balance(updated.getBalance()).replayed(false).build();
    }

    @Override
    public AccountOperationResult compensate(UUID accountId, String originalIdempotencyKey, UUID sagaId,
                                              String idempotencyKey) {
        Optional<IdempotencyKeyJpaEntity> existing = idempotencyStore.find(idempotencyKey);
        if (existing.isPresent()) {
            idempotencyStore.verifyReplayMatches(existing.get(), accountId, IdempotentOperationType.COMPENSATE);
            log.info("Idempotent replay of compensate: key={}, accountId={}, sagaId={}", idempotencyKey, accountId, sagaId);
            return result(existing.get(), true);
        }
        IdempotencyKeyJpaEntity originalDebit = idempotencyStore.find(originalIdempotencyKey)
                .filter(record -> record.getOperation() == IdempotentOperationType.DEBIT)
                .filter(record -> record.getAccountId().equals(accountId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No matching debit found for idempotency key " + originalIdempotencyKey
                                + " on account " + accountId));
        BigDecimal amount = originalDebit.getAmount();
        String currency = originalDebit.getCurrency();
        BankAccount updated = applyWithOptimisticRetry(accountId, currency,
                account -> account.setBalance(account.getBalance().add(amount)));
        idempotencyStore.record(idempotencyKey, accountId, IdempotentOperationType.COMPENSATE, sagaId, amount,
                currency, updated.getBalance());
        log.warn("Compensated debit for accountId={}, amount={}, sagaId={}, idempotencyKey={}, newBalance={}",
                accountId, amount, sagaId, idempotencyKey, updated.getBalance());
        return AccountOperationResult.builder().accountId(accountId).balance(updated.getBalance()).replayed(false).build();
    }

    private AccountOperationResult result(IdempotencyKeyJpaEntity record, boolean replayed) {
        return AccountOperationResult.builder()
                .accountId(record.getAccountId())
                .balance(record.getBalanceAfter())
                .replayed(replayed)
                .build();
    }

    /**
     * Applies {@code mutation} to the account, retrying on an optimistic-lock conflict from the
     * {@code @Version} column. Business rejections thrown by {@code mutation} (insufficient funds,
     * etc.) propagate immediately and are never retried.
     */
    private BankAccount applyWithOptimisticRetry(UUID accountId, String expectedCurrency, Consumer<BankAccount> mutation) {
        int attempt = 0;
        while (true) {
            attempt++;
            BankAccount account = accountRepositoryPort.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bank account " + accountId + " not found"));
            if (account.getStatus() != AccountStatus.ACTIVE) {
                throw new InvalidAccountStateException(
                        "Account " + accountId + " is not ACTIVE (status=" + account.getStatus() + ")");
            }
            if (!account.getCurrency().equals(expectedCurrency)) {
                throw new InvalidAccountStateException("Account " + accountId + " currency " + account.getCurrency()
                        + " does not match expected currency " + expectedCurrency);
            }
            mutation.accept(account);
            try {
                return accountRepositoryPort.save(account);
            } catch (ObjectOptimisticLockingFailureException ex) {
                if (attempt >= MAX_OPTIMISTIC_LOCK_ATTEMPTS) {
                    throw ex;
                }
                log.warn("Optimistic lock conflict updating account {}, retrying (attempt {})", accountId, attempt);
            }
        }
    }
}
