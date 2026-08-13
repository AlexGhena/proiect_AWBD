package bankingService.demo.application.service;

import bankingService.demo.domain.exception.DuplicateResourceException;
import bankingService.demo.domain.exception.InvalidCardStateException;
import bankingService.demo.domain.exception.ResourceNotFoundException;
import bankingService.demo.domain.model.BankAccount;
import bankingService.demo.domain.model.BankCard;
import bankingService.demo.domain.model.CardStatus;
import bankingService.demo.domain.model.RevealedCardDetails;
import bankingService.demo.domain.port.in.CardUseCase;
import bankingService.demo.domain.port.out.AccountRepositoryPort;
import bankingService.demo.domain.port.out.CardRepositoryPort;
import bankingService.demo.security.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CardService implements CardUseCase {

    private final CardRepositoryPort cardRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final UserServiceClient userServiceClient;
    private final CardIssuanceGenerator cardIssuanceGenerator;

    @Override
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.ownsAccount(#card.accountId)")
    public BankCard createCard(BankCard card) {
        if (!accountRepositoryPort.existsById(card.getAccountId())) {
            throw new ResourceNotFoundException("Bank account " + card.getAccountId() + " not found");
        }
        if (cardRepositoryPort.existsByCardReference(card.getCardReference())) {
            throw new DuplicateResourceException(
                    "Bank card with reference " + card.getCardReference() + " already exists");
        }
        card.setId(null);
        if (card.getStatus() == null) {
            card.setStatus(CardStatus.ACTIVE);
        }
        if (card.getCardNumber() == null) {
            card.setCardNumber(cardIssuanceGenerator.cardNumberEndingIn(card.getLastFour()));
        }
        if (card.getCvv() == null) {
            card.setCvv(cardIssuanceGenerator.cvv());
        }
        if (card.getPin() == null) {
            card.setPin(cardIssuanceGenerator.pin());
        }
        BankCard saved = cardRepositoryPort.save(card);
        log.info("Bank card created with id={}, accountId={}", saved.getId(), saved.getAccountId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.ownsCard(#id)")
    public BankCard getCard(UUID id) {
        return cardRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank card " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<BankCard> listCards(Pageable pageable) {
        log.debug("Listing bank cards with pageable={}", pageable);
        return cardRepositoryPort.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.ownsAccount(#accountId)")
    public List<BankCard> listCardsByAccount(UUID accountId) {
        if (!accountRepositoryPort.existsById(accountId)) {
            throw new ResourceNotFoundException("Bank account " + accountId + " not found");
        }
        return cardRepositoryPort.findByAccountId(accountId);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.ownsCard(#id)")
    public BankCard updateCard(UUID id, BankCard updates) {
        BankCard existing = getCard(id);
        if (existing.getStatus() == CardStatus.LOST_STOLEN) {
            throw new InvalidCardStateException(
                    "Card " + id + " was reported lost/stolen and can no longer be modified");
        }
        if (updates.getStatus() == CardStatus.LOST_STOLEN) {
            throw new InvalidCardStateException("Use the report-lost action to mark a card lost or stolen");
        }
        if (updates.getCardholderName() != null) {
            existing.setCardholderName(updates.getCardholderName());
        }
        if (updates.getExpiryMonth() != null) {
            existing.setExpiryMonth(updates.getExpiryMonth());
        }
        if (updates.getExpiryYear() != null) {
            existing.setExpiryYear(updates.getExpiryYear());
        }
        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }
        BankCard saved = cardRepositoryPort.save(existing);
        log.info("Bank card updated with id={}", saved.getId());
        return saved;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.ownsCard(#id)")
    public void deleteCard(UUID id) {
        if (!cardRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Bank card " + id + " not found");
        }
        cardRepositoryPort.deleteById(id);
        log.info("Bank card deleted with id={}", id);
    }

    /**
     * Self-service only - not admin-overridable, since password confirmation only proves something
     * when it is the card owner's own credentials being checked.
     */
    @Override
    @PreAuthorize("@accountSecurity.ownsCard(#id)")
    public RevealedCardDetails revealDetails(UUID id, String password) {
        BankCard card = getCard(id);
        verifyOwnerPassword(card, password);
        ensureSensitiveFieldsPresent(card);
        return new RevealedCardDetails(card.getCardNumber(), card.getCvv(), card.getCardholderName(),
                card.getExpiryMonth(), card.getExpiryYear());
    }

    @Override
    @PreAuthorize("@accountSecurity.ownsCard(#id)")
    public String revealPin(UUID id, String password) {
        BankCard card = getCard(id);
        verifyOwnerPassword(card, password);
        ensureSensitiveFieldsPresent(card);
        return card.getPin();
    }

    @Override
    @PreAuthorize("@accountSecurity.ownsCard(#id)")
    public BankCard changePin(UUID id, String currentPassword, String newPin) {
        BankCard card = getCard(id);
        if (card.getStatus() == CardStatus.LOST_STOLEN) {
            throw new InvalidCardStateException(
                    "Card " + id + " was reported lost/stolen and can no longer be modified");
        }
        verifyOwnerPassword(card, currentPassword);
        // Backfill cardNumber/cvv first if this is a legacy card - otherwise a later reveal would see
        // them still missing and regenerate all three fields, silently overwriting the new PIN.
        ensureSensitiveFieldsPresent(card);
        card.setPin(newPin);
        BankCard saved = cardRepositoryPort.save(card);
        log.info("PIN changed for card id={}", id);
        return saved;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.ownsCard(#id)")
    public BankCard reportLostOrStolen(UUID id) {
        BankCard card = getCard(id);
        if (card.getStatus() == CardStatus.LOST_STOLEN) {
            throw new InvalidCardStateException("Card " + id + " was already reported lost/stolen");
        }
        card.setStatus(CardStatus.LOST_STOLEN);
        BankCard saved = cardRepositoryPort.save(card);
        log.warn("Card id={} reported lost/stolen", id);
        return saved;
    }

    private void verifyOwnerPassword(BankCard card, String password) {
        BankAccount account = accountRepositoryPort.findById(card.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank account " + card.getAccountId() + " not found"));
        if (!userServiceClient.verifyPassword(account.getUserId(), password)) {
            throw new AccessDeniedException("Incorrect password");
        }
    }

    /** Lazily backfills PAN/CVV/PIN for cards issued before these fields existed. */
    private void ensureSensitiveFieldsPresent(BankCard card) {
        if (card.getCardNumber() != null && card.getCvv() != null && card.getPin() != null) {
            return;
        }
        card.setCardNumber(cardIssuanceGenerator.cardNumberEndingIn(card.getLastFour()));
        card.setCvv(cardIssuanceGenerator.cvv());
        card.setPin(cardIssuanceGenerator.pin());
        cardRepositoryPort.save(card);
        log.info("Backfilled sensitive fields for legacy card id={}", card.getId());
    }
}
