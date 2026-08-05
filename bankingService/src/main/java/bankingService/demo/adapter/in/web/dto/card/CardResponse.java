package bankingService.demo.adapter.in.web.dto.card;

import bankingService.demo.domain.model.CardStatus;

import java.time.Instant;
import java.util.UUID;

public record CardResponse(
        UUID id,
        UUID accountId,
        String cardReference,
        String lastFour,
        String cardholderName,
        Integer expiryMonth,
        Integer expiryYear,
        CardStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
