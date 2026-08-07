package userService.demo.support;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Generates a throwaway RSA key pair for tests.
 *
 * <p>Keys are created fresh in memory on every run, so no development or production key material is
 * ever involved in, or committed alongside, the test suite.
 */
public final class TestRsaKeys {

    private final KeyPair keyPair;

    private TestRsaKeys(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public static TestRsaKeys generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return new TestRsaKeys(generator.generateKeyPair());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA key generation is unavailable on this JVM", ex);
        }
    }

    public String privateKeyPem() {
        return pem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
    }

    public String publicKeyPem() {
        return pem("PUBLIC KEY", keyPair.getPublic().getEncoded());
    }

    public KeyPair keyPair() {
        return keyPair;
    }

    private static String pem(String label, byte[] der) {
        return "-----BEGIN " + label + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der)
                + "\n-----END " + label + "-----\n";
    }
}
