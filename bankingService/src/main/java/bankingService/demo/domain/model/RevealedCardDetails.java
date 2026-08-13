package bankingService.demo.domain.model;

/** The full PAN/CVV, released only after the caller re-confirms their account password. */
public record RevealedCardDetails(
        String cardNumber,
        String cvv,
        String cardholderName,
        Integer expiryMonth,
        Integer expiryYear
) {
}
