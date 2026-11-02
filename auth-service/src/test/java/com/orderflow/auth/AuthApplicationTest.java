package com.orderflow.auth;

import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthApplicationTest {
    @Test void hashesPasswords() {
        var encoder = new BCryptPasswordEncoder(4);
        var hash = encoder.encode("correct-horse");
        assertNotEquals("correct-horse", hash);
        assertTrue(encoder.matches("correct-horse", hash));
    }

    @Test void rejectsWrongPasswordWithoutIssuingTokens() {
        UserRepo users = mock(UserRepo.class);
        TokenRepo tokens = mock(TokenRepo.class);
        var encoder = new BCryptPasswordEncoder(4);
        when(users.findByEmail("student@example.com"))
                .thenReturn(Optional.of(new User("student@example.com", encoder.encode("correct-password"))));
        var service = new AuthService(users, tokens, encoder,
                "test-secret-that-is-long-enough-for-hmac", 15, 7);

        var error = assertThrows(ResponseStatusException.class,
                () -> service.login("student@example.com", "wrong-password"));

        assertEquals(401, error.getStatusCode().value());
        verifyNoInteractions(tokens);
    }

    @Test void rejectsUnknownOrReplayedRefreshToken() {
        TokenRepo tokens = mock(TokenRepo.class);
        when(tokens.findByTokenHashAndRevokedFalse(anyString())).thenReturn(Optional.empty());
        var service = new AuthService(mock(UserRepo.class), tokens, new BCryptPasswordEncoder(4),
                "test-secret-that-is-long-enough-for-hmac", 15, 7);
        var error = assertThrows(ResponseStatusException.class, () -> service.refresh("already-used"));
        assertEquals(401, error.getStatusCode().value());
    }

    @Test void malformedCredentialsFailBeanValidation() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var violations = validatorFactory.getValidator()
                    .validate(new AuthController.Credentials("not-an-email", "short"));
            assertEquals(2, violations.size());
        }
    }
}
