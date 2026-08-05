package bankingService.demo.application.service;

import bankingService.demo.domain.exception.DuplicateResourceException;
import bankingService.demo.domain.exception.ResourceNotFoundException;
import bankingService.demo.domain.model.BankCard;
import bankingService.demo.domain.model.CardStatus;
import bankingService.demo.domain.port.in.CardUseCase;
import bankingService.demo.domain.port.out.AccountRepositoryPort;
import bankingService.demo.domain.port.out.CardRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CardService implements CardUseCase {

    private final CardRepositoryPort cardRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;

    public CardService(CardRepositoryPort cardRepositoryPort, AccountRepositoryPort accountRepositoryPort) {
        this.cardRepositoryPort = cardRepositoryPort;
        this.accountRepositoryPort = accountRepositoryPort;
    }

    @Override
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
        return cardRepositoryPort.save(card);
    }

    @Override
    @Transactional(readOnly = true)
    public BankCard getCard(UUID id) {
        return cardRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank card " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BankCard> listCards(Pageable pageable) {
        return cardRepositoryPort.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankCard> listCardsByAccount(UUID accountId) {
        if (!accountRepositoryPort.existsById(accountId)) {
            throw new ResourceNotFoundException("Bank account " + accountId + " not found");
        }
        return cardRepositoryPort.findByAccountId(accountId);
    }

    @Override
    public BankCard updateCard(UUID id, BankCard updates) {
        BankCard existing = getCard(id);
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
        return cardRepositoryPort.save(existing);
    }

    @Override
    public void deleteCard(UUID id) {
        if (!cardRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Bank card " + id + " not found");
        }
        cardRepositoryPort.deleteById(id);
    }
}
