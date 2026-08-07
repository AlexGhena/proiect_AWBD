package userService.demo.config;

import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.UUID;

/**
 * Resolves the RSA key pair used to sign access tokens.
 *
 * <p>Production supplies both keys as PEM through {@link JwtProperties}. When they are absent the
 * provider falls back to a generated key pair cached under {@code .keys/} so a developer machine
 * keeps a stable {@code kid} across restarts without any key material ever being committed.
 */
@Slf4j
final class RsaKeyProvider {

    private static final Path DEV_KEY_FILE = Path.of(".keys", "jwt-dev.jwk");

    private RsaKeyProvider() {
    }

    static RSAKey resolve(JwtProperties properties) {
        if (properties.hasConfiguredKeyPair()) {
            log.info("Loading the JWT signing key pair from configuration");
            return fromPem(properties.privateKey(), properties.publicKey());
        }
        log.warn("No JWT key pair configured; falling back to a locally generated development key. "
                + "Set app.security.jwt.private-key/public-key outside of local development.");
        return devKeyPair();
    }

    private static RSAKey fromPem(String privateKeyPem, String publicKeyPem) {
        try {
            RSAPrivateKey privateKey = (RSAPrivateKey) java.security.KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decodePem(privateKeyPem)));
            RSAPublicKey publicKey = (RSAPublicKey) java.security.KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decodePem(publicKeyPem)));
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.nameUUIDFromBytes(publicKey.getEncoded()).toString())
                    .build();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("The configured JWT key pair could not be parsed", ex);
        }
    }

    /** Strips the PEM armour and whitespace, leaving the base64 DER payload. */
    private static byte[] decodePem(String pem) {
        String base64 = pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    private static RSAKey devKeyPair() {
        RSAKey cached = readCachedDevKey();
        if (cached != null) {
            return cached;
        }
        RSAKey generated = generate();
        writeCachedDevKey(generated);
        return generated;
    }

    private static RSAKey readCachedDevKey() {
        if (!Files.isRegularFile(DEV_KEY_FILE)) {
            return null;
        }
        try {
            return RSAKey.parse(Files.readString(DEV_KEY_FILE, StandardCharsets.UTF_8));
        } catch (IOException | ParseException ex) {
            log.warn("Cached development key at {} is unreadable; generating a new one", DEV_KEY_FILE);
            return null;
        }
    }

    private static void writeCachedDevKey(RSAKey key) {
        try {
            Files.createDirectories(DEV_KEY_FILE.getParent());
            Files.writeString(DEV_KEY_FILE, key.toJSONString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            // Not fatal: the service still runs, tokens simply stop verifying after a restart.
            log.warn("Could not cache the development key at {}: {}", DEV_KEY_FILE, ex.getMessage());
        }
    }

    private static RSAKey generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA key generation is unavailable on this JVM", ex);
        }
    }
}
