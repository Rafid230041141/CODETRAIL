package application.backend.web;

import application.backend.dto.AuthRequest;
import application.backend.dto.AuthResponse;
import application.backend.dto.LoginRequest;
import application.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    public record GoogleAuthRequest(String email, String displayName) {}
    public record ForgotPasswordRequest(String emailOrUsername) {}
    public record ResetPasswordRequest(String emailOrUsername, String code, String newPassword) {}
    public record AuthMessageResponse(String message, String code) {}

    @PostMapping("/google")
    public AuthResponse googleLogin(@RequestBody GoogleAuthRequest request) {
        return auth.googleLogin(request.email(), request.displayName());
    }

    @PostMapping("/forgot-password")
    public AuthMessageResponse forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String code = auth.requestPasswordReset(request.emailOrUsername());
        return new AuthMessageResponse("A 6-digit verification code has been dispatched to your email address.", code);
    }

    @PostMapping("/reset-password")
    public AuthMessageResponse resetPassword(@RequestBody ResetPasswordRequest request) {
        auth.resetPassword(request.emailOrUsername(), request.code(), request.newPassword());
        return new AuthMessageResponse("Password updated successfully. You may now sign in.", null);
    }
}
