package bankingService.demo.adapter.in.web.dto.card;

public record CardDetailsResponse(
        String cardNumber,
        String cvv,
        String cardholderName,
        Integer expiryMonth,
        Integer expiryYear
) {
}
