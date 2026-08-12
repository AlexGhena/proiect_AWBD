package bankingService.demo.application.service;

import bankingService.demo.domain.port.out.AccountRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** ISO 7064 mod-97 checksum correctness and shape of the generated Romanian IBAN. */
class IbanGeneratorTests {

    private final AccountRepositoryPort accountRepositoryPort = mock(AccountRepositoryPort.class);
    private final IbanGenerator generator = new IbanGenerator(accountRepositoryPort);

    @Test
    @DisplayName("A generated IBAN is 24 characters: RO + 2 check digits + AWBD + 16 digits")
    void generatesExpectedShape() {
        when(accountRepositoryPort.existsByIban(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        String iban = generator.generate();

        assertThat(iban).hasSize(24);
        assertThat(iban).startsWith("RO");
        assertThat(iban.substring(4, 8)).isEqualTo("AWBD");
        assertThat(iban.substring(8)).matches("\\d{16}");
        assertThat(iban.substring(2, 4)).matches("\\d{2}");
    }

    @Test
    @DisplayName("The check digits satisfy the ISO 7064 mod-97 IBAN checksum")
    void checkDigitsSatisfyMod97() {
        when(accountRepositoryPort.existsByIban(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        String iban = generator.generate();

        assertThat(mod97(iban)).isEqualTo(1);
    }

    @Test
    @DisplayName("A collision is retried until a free IBAN is found")
    void retriesOnCollision() {
        when(accountRepositoryPort.existsByIban(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true, true, false);

        String iban = generator.generate();

        assertThat(iban).hasSize(24);
    }

    @Test
    @DisplayName("Repeated generation produces distinct IBANs")
    void generatesDistinctValues() {
        when(accountRepositoryPort.existsByIban(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            generated.add(generator.generate());
        }

        assertThat(generated).hasSize(50);
    }

    /** Standard IBAN validation: move the first 4 characters to the end, letters -> numbers, mod 97 must equal 1. */
    private static int mod97(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            numeric.append(Character.isDigit(c) ? String.valueOf(c) : String.valueOf(Character.getNumericValue(c)));
        }
        return new BigInteger(numeric.toString()).mod(BigInteger.valueOf(97)).intValue();
    }
}
