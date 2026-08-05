package bankingService.demo.adapter.in.web.dto.card;

import bankingService.demo.domain.model.CardStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateCardRequest(
        String cardholderName,
        @Min(1) @Max(12) Integer expiryMonth,
        Integer expiryYear,
        CardStatus status
) {
}
