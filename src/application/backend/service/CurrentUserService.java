package application.backend.service;

import application.backend.domain.UserAccount;
import application.backend.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserAccountRepository users;

    public CurrentUserService(UserAccountRepository users) {
        this.users = users;
    }

    public UserAccount require(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new UnauthorizedException("Authentication is required");
        }
        return users.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Authenticated account no longer exists"));
    }
}
