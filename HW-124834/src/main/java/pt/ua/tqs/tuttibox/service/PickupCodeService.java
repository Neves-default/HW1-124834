package pt.ua.tqs.tuttibox.service;

import org.springframework.stereotype.Service;
import pt.ua.tqs.tuttibox.config.TuttiboxProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Pickup codes: N random digits (SecureRandom). Only an HMAC of (station, code) is stored,
 * so the plain code is never in the database and can still be looked up when entered.
 */
@Service
public class PickupCodeService {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecureRandom random = new SecureRandom();
    private final int length;
    private final byte[] secret;

    public PickupCodeService(TuttiboxProperties properties) {
        this.length = properties.pickupCode().length();
        this.secret = properties.pickupCode().secret().getBytes(StandardCharsets.UTF_8);
    }

    public String newCode() {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    public boolean isWellFormed(String code) {
        return code != null && code.matches("\\d{" + length + "}");
    }

    public String hash(long stationId, String code) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            byte[] digest = mac.doFinal((stationId + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC not available", e);
        }
    }
}
