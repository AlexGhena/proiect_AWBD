package bankingService.demo.application.service;

import bankingService.demo.domain.port.out.AccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Generates Romanian-format IBANs: {@code RO} + 2 ISO 7064 mod-97 check digits + a 4-letter bank
 * code + a 16-digit account number (24 characters total, matching real RO IBANs).
 */
@Component
@RequiredArgsConstructor
class IbanGenerator {

    private static final String COUNTRY_CODE = "RO";
    private static final String BANK_CODE = "AWBD";
    private static final int ACCOUNT_DIGITS = 16;
    private static final int MAX_ATTEMPTS = 20;

    private final AccountRepositoryPort accountRepositoryPort;
    private final SecureRandom random = new SecureRandom();

    /** Retries on a random collision; with 10^16 possible account numbers this is a formality. */
    String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = candidate();
            if (!accountRepositoryPort.existsByIban(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique IBAN after " + MAX_ATTEMPTS + " attempts");
    }

    private String candidate() {
        StringBuilder accountNumber = new StringBuilder(ACCOUNT_DIGITS);
        for (int i = 0; i < ACCOUNT_DIGITS; i++) {
            accountNumber.append(random.nextInt(10));
        }
        String bban = BANK_CODE + accountNumber;
        return COUNTRY_CODE + checkDigits(bban) + bban;
    }

    /**
     * ISO 7064 mod-97-10: move the BBAN in front, append the country code and "00" in place of the
     * check digits, convert letters to numbers (A=10..Z=35), then check = 98 - (numeric mod 97).
     */
    private String checkDigits(String bban) {
        String rearranged = bban + COUNTRY_CODE + "00";
        BigInteger numeric = new BigInteger(toNumericString(rearranged));
        int check = 98 - numeric.mod(BigInteger.valueOf(97)).intValue();
        return String.format("%02d", check);
    }

    private String toNumericString(String value) {
        StringBuilder numeric = new StringBuilder(value.length() * 2);
        for (char c : value.toCharArray()) {
            if (Character.isDigit(c)) {
                numeric.append(c);
            } else {
                numeric.append(Character.getNumericValue(c));
            }
        }
        return numeric.toString();
    }
}
