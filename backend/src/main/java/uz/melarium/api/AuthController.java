package uz.melarium.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import uz.melarium.api.dto.LogoutRequest;
import uz.melarium.api.dto.OtpRequest;
import uz.melarium.api.dto.OtpRequestResponse;
import uz.melarium.api.dto.OtpVerifyRequest;
import uz.melarium.api.dto.RefreshRequest;
import uz.melarium.api.dto.TokenResponse;
import uz.melarium.auth.AuthService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/otp/request")
    public ResponseEntity<OtpRequestResponse> requestOtp(@Valid @RequestBody OtpRequest body, HttpServletRequest request) {
        OtpRequestResponse response = authService.requestOtp(body.phone(), request.getRemoteAddr());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(response);
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<TokenResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(authService.verifyOtp(body.phone(), body.code()));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(authService.refresh(body.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest body) {
        authService.logout(body.refreshToken());
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }
}
