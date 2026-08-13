package bankingService.demo.application.service;

import bankingService.demo.domain.port.out.CardRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates the card reference, PAN, CVV and PIN for a debit card issued automatically when an
 * account is opened, mirroring {@link IbanGenerator}'s collision-retry approach.
 */
@Component
@RequiredArgsConstructor
class CardIssuanceGenerator {

    private static final String HEX_ALPHABET = "0123456789ABCDEF";
    private static final int REFERENCE_HEX_LENGTH = 12;
    private static final int MAX_ATTEMPTS = 20;
    /** Cosmetic BIN prefix only - not a real card scheme, just makes the generated PAN look card-shaped. */
    private static final String CARD_NUMBER_PREFIX = "4532";
    private static final int CARD_NUMBER_LENGTH = 16;

    private final CardRepositoryPort cardRepositoryPort;
    private final SecureRandom random = new SecureRandom();

    /** Retries on a random collision; with 16^12 possible tokens this is a formality. */
    String cardReference() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = "TOK-" + randomHex(REFERENCE_HEX_LENGTH);
            if (!cardRepositoryPort.existsByCardReference(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not generate a unique card reference after " + MAX_ATTEMPTS + " attempts");
    }

    /** The 4 digits shown unmasked as the card's last four; also embedded as the PAN's tail. */
    String lastFour() {
        return randomDigits(4);
    }

    /** A cosmetic 16-digit PAN ending in the given last-four digits, so the two stay consistent. */
    String cardNumberEndingIn(String lastFour) {
        int middleLength = CARD_NUMBER_LENGTH - CARD_NUMBER_PREFIX.length() - lastFour.length();
        return CARD_NUMBER_PREFIX + randomDigits(middleLength) + lastFour;
    }

    String cvv() {
        return randomDigits(3);
    }

    String pin() {
        return randomDigits(4);
    }

    private String randomDigits(int length) {
        StringBuilder digits = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            digits.append(random.nextInt(10));
        }
        return digits.toString();
    }

    private String randomHex(int length) {
        StringBuilder hex = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            hex.append(HEX_ALPHABET.charAt(random.nextInt(HEX_ALPHABET.length())));
        }
        return hex.toString();
    }
}
