package bankingService.demo.adapter.in.web.mapper;

import bankingService.demo.adapter.in.web.dto.card.CardResponse;
import bankingService.demo.adapter.in.web.dto.card.CreateCardRequest;
import bankingService.demo.adapter.in.web.dto.card.UpdateCardRequest;
import bankingService.demo.domain.model.BankCard;
import org.springframework.stereotype.Component;

@Component
public class CardWebMapper {

    public BankCard toDomain(CreateCardRequest request) {
        return BankCard.builder()
                .accountId(request.accountId())
                .cardReference(request.cardReference())
                .lastFour(request.lastFour())
                .cardholderName(request.cardholderName())
                .expiryMonth(request.expiryMonth())
                .expiryYear(request.expiryYear())
                .build();
    }

    public BankCard toDomain(UpdateCardRequest request) {
        return BankCard.builder()
                .cardholderName(request.cardholderName())
                .expiryMonth(request.expiryMonth())
                .expiryYear(request.expiryYear())
                .status(request.status())
                .build();
    }

    public CardResponse toResponse(BankCard domain) {
        return new CardResponse(
                domain.getId(),
                domain.getAccountId(),
                domain.getCardReference(),
                domain.getLastFour(),
                domain.getCardholderName(),
                domain.getExpiryMonth(),
                domain.getExpiryYear(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
