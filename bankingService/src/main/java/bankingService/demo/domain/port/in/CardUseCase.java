package bankingService.demo.domain.port.in;

import bankingService.demo.domain.model.BankCard;
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
}
