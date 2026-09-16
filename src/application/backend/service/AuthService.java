package application.backend.service;

import application.backend.domain.Role;
import application.backend.domain.UserAccount;
import application.backend.dto.AuthRequest;
import application.backend.dto.AuthResponse;
import application.backend.dto.LoginRequest;
import application.backend.repository.UserAccountRepository;
import application.backend.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;
    private final EmailService emailService;

    public AuthService(UserAccountRepository users, PasswordEncoder passwordEncoder, JwtTokenService tokens, EmailService emailService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.emailService = emailService;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        String username = clean(request.username());
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username is already registered");
        }
        String displayName = clean(request.displayName());
        UserAccount user = new UserAccount(username, displayName,
                passwordEncoder.encode(request.password()), request.role() == null ? Role.STUDENT : request.role(),
                username.contains("@") ? username : null);
        user = users.save(user);
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount user = users.findByUsernameIgnoreCase(clean(request.username()))
                .or(() -> users.findByEmailIgnoreCase(clean(request.username())))
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }
        return response(user);
    }

    @Transactional
    public AuthResponse googleLogin(String email, String displayName) {
        String cleanEmail = clean(email).toLowerCase(java.util.Locale.ROOT);
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            throw new BadRequestException("A valid Google / Gmail address is required.");
        }
        String cleanName = clean(displayName);
        if (cleanName.isBlank()) {
            cleanName = cleanEmail.substring(0, cleanEmail.indexOf('@'));
        }
        UserAccount user = users.findByEmailIgnoreCase(cleanEmail)
                .or(() -> users.findByUsernameIgnoreCase(cleanEmail))
                .orElse(null);

        if (user == null) {
            String randomPassword = java.util.UUID.randomUUID().toString();
            user = new UserAccount(cleanEmail, cleanName, passwordEncoder.encode(randomPassword), Role.STUDENT, cleanEmail);
            user = users.save(user);
        }
        return response(user);
    }

    @Transactional
    public String requestPasswordReset(String emailOrUsername) {
        String cleanId = clean(emailOrUsername);
        if (cleanId.isBlank()) {
            throw new BadRequestException("Enter your username or email address.");
        }
        UserAccount user = users.findByUsernameIgnoreCase(cleanId)
                .or(() -> users.findByEmailIgnoreCase(cleanId))
                .orElseThrow(() -> new NotFoundException("No account found for: " + cleanId));

        int codeInt = 100000 + new java.security.SecureRandom().nextInt(900000);
        String code = String.valueOf(codeInt);
        user.setPasswordResetCode(code);
        user.setPasswordResetExpiry(java.time.Instant.now().plus(java.time.Duration.ofMinutes(15)));
        users.save(user);

        String targetEmail = user.getEmail();
        if (targetEmail.isBlank()) {
            targetEmail = user.getUsername();
        }
        emailService.sendPasswordResetEmail(targetEmail, user.getDisplayName(), code);
        return code;
    }

    @Transactional
    public void resetPassword(String emailOrUsername, String code, String newPassword) {
        String cleanId = clean(emailOrUsername);
        UserAccount user = users.findByUsernameIgnoreCase(cleanId)
                .or(() -> users.findByEmailIgnoreCase(cleanId))
                .orElseThrow(() -> new NotFoundException("No account found for: " + cleanId));

        if (user.getPasswordResetCode() == null || !user.getPasswordResetCode().equals(clean(code))) {
            throw new BadRequestException("Invalid or incorrect verification code.");
        }
        if (user.getPasswordResetExpiry() == null || java.time.Instant.now().isAfter(user.getPasswordResetExpiry())) {
            throw new BadRequestException("Verification code has expired. Please request a new code.");
        }
        if (newPassword == null || newPassword.trim().length() < 4) {
            throw new BadRequestException("Password must be at least 4 characters.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        user.setPasswordResetCode(null);
        user.setPasswordResetExpiry(null);
        users.save(user);
    }

    private AuthResponse response(UserAccount user) {
        return new AuthResponse(tokens.issue(user), user.getRole().name(), user.getId(), user.getUsername(), user.getDisplayName());
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
