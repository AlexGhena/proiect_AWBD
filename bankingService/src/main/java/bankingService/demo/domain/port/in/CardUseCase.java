package bankingService.demo.domain.port.in;

import bankingService.demo.domain.model.BankCard;
import bankingService.demo.domain.model.RevealedCardDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CardUseCase {

    BankCard createCard(BankCard card);

    BankCard getCard(UUID id);

    Page<BankCard> listCards(Pageable pageable);

    List<BankCard> listCardsByAccount(UUID accountId);

    BankCard updateCard(UUID id, BankCard updates);

    void deleteCard(UUID id);

    /** Full PAN/CVV, gated by the card owner re-confirming their account password. */
    RevealedCardDetails revealDetails(UUID id, String password);

    /** Current PIN, gated by the card owner re-confirming their account password. */
    String revealPin(UUID id, String password);

    /** Sets a new PIN, gated by the card owner re-confirming their current account password. */
    BankCard changePin(UUID id, String currentPassword, String newPin);

    /** Terminal: marks the card LOST_STOLEN. Not reversible through {@link #updateCard}. */
    BankCard reportLostOrStolen(UUID id);
}
