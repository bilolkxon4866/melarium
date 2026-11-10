package uz.melarium.auth;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile({"staging", "prod"})
public class HttpSmsSender implements SmsSender {
    private final RestClient client;
    private final String apiUrl;
    private final String apiToken;
    private final String senderId;

    public HttpSmsSender(
            RestClient.Builder builder,
            @Value("${app.sms.api-url}") String apiUrl,
            @Value("${app.sms.api-token}") String apiToken,
            @Value("${app.sms.sender-id}") String senderId
    ) {
        if (apiUrl.isBlank() || apiToken.isBlank()) {
            throw new IllegalStateException("SMS_API_URL va SMS_API_TOKEN staging/production’da majburiy");
        }
        this.client = builder.baseUrl(apiUrl).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken).build();
        this.apiUrl = apiUrl;
        this.apiToken = apiToken;
        this.senderId = senderId;
    }

    @Override
    public void sendOtp(String phone, String code) {
        client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("to", phone, "from", senderId, "text", "MELARIUM tasdiqlash kodi: " + code))
                .retrieve()
                .toBodilessEntity();
    }
}
