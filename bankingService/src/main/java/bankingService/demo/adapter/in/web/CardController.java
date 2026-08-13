package bankingService.demo.adapter.in.web;

import bankingService.demo.adapter.in.web.dto.card.CardDetailsResponse;
import bankingService.demo.adapter.in.web.dto.card.CardPinResponse;
import bankingService.demo.adapter.in.web.dto.card.CardResponse;
import bankingService.demo.adapter.in.web.dto.card.ChangePinRequest;
import bankingService.demo.adapter.in.web.dto.card.CreateCardRequest;
import bankingService.demo.adapter.in.web.dto.card.PasswordConfirmationRequest;
import bankingService.demo.adapter.in.web.dto.card.UpdateCardRequest;
import bankingService.demo.adapter.in.web.mapper.CardWebMapper;
import bankingService.demo.domain.model.BankCard;
import bankingService.demo.domain.model.RevealedCardDetails;
import bankingService.demo.domain.port.in.CardUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CardController {

    private final CardUseCase cardUseCase;
    private final CardWebMapper mapper;

    @PostMapping("/api/cards")
    public ResponseEntity<CardResponse> create(@Valid @RequestBody CreateCardRequest request) {
        BankCard created = cardUseCase.createCard(mapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @GetMapping("/api/cards/{id}")
    public ResponseEntity<CardResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(cardUseCase.getCard(id)));
    }

    @GetMapping("/api/cards")
    public ResponseEntity<PagedModel<CardResponse>> list(Pageable pageable) {
        Page<CardResponse> page = cardUseCase.listCards(pageable).map(mapper::toResponse);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @GetMapping("/api/accounts/{accountId}/cards")
    public ResponseEntity<List<CardResponse>> listByAccount(@PathVariable UUID accountId) {
        List<CardResponse> cards = cardUseCase.listCardsByAccount(accountId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(cards);
    }

    @PutMapping("/api/cards/{id}")
    public ResponseEntity<CardResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateCardRequest request) {
        BankCard updated = cardUseCase.updateCard(id, mapper.toDomain(request));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/api/cards/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        cardUseCase.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/cards/{id}/reveal")
    public ResponseEntity<CardDetailsResponse> reveal(@PathVariable UUID id,
                                                         @Valid @RequestBody PasswordConfirmationRequest request) {
        RevealedCardDetails details = cardUseCase.revealDetails(id, request.password());
        return ResponseEntity.ok(new CardDetailsResponse(details.cardNumber(), details.cvv(),
                details.cardholderName(), details.expiryMonth(), details.expiryYear()));
    }

    @PostMapping("/api/cards/{id}/pin/reveal")
    public ResponseEntity<CardPinResponse> revealPin(@PathVariable UUID id,
                                                        @Valid @RequestBody PasswordConfirmationRequest request) {
        String pin = cardUseCase.revealPin(id, request.password());
        return ResponseEntity.ok(new CardPinResponse(pin));
    }

    @PutMapping("/api/cards/{id}/pin")
    public ResponseEntity<Void> changePin(@PathVariable UUID id, @Valid @RequestBody ChangePinRequest request) {
        cardUseCase.changePin(id, request.currentPassword(), request.newPin());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/cards/{id}/report-lost")
    public ResponseEntity<CardResponse> reportLost(@PathVariable UUID id) {
        BankCard updated = cardUseCase.reportLostOrStolen(id);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
