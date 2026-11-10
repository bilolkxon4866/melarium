package uz.melarium.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OtpHasher {
    private final byte[] secret;

    public OtpHasher(@Value("${app.security.otp-pepper-base64}") String encodedSecret) {
        this.secret = Base64.getDecoder().decode(encodedSecret);
        if (secret.length < 32) throw new IllegalStateException("OTP pepper kamida 32 byte bo‘lishi kerak");
    }

    public String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("OTP hash hisoblanmadi", exception);
        }
    }
}
