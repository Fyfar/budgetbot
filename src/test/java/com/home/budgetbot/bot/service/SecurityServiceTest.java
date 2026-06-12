package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.ConfigRepository;
import com.home.budgetbot.bot.repository.entity.config.SecurityConfigEntity;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(environments = {"integration", "disableTelegramBot"})
class SecurityServiceTest {

    @Inject
    SecurityService securityService;

    @Inject
    ConfigRepository configRepository;

    @BeforeEach
    void clearAuthorizedUsers() {
        SecurityConfigEntity config = configRepository.getSecurityConfig();
        config.getAuthorizedUserList().clear();
        configRepository.update(config);
    }

    @Test
    void firstUserBecomesAdmin() {
        User user = buildUser(42L, "firstUser");

        boolean result = securityService.isAuthorizedUser(user);

        assertTrue(result);
        SecurityConfigEntity config = configRepository.getSecurityConfig();
        assertTrue(config.getAuthorizedUserList().contains(42));
    }

    @Test
    void authorizedUserIsAllowed() {
        securityService.addAuthorizedUser(99);

        User user = buildUser(99L, "knownUser");

        assertTrue(securityService.isAuthorizedUser(user));
    }

    @Test
    void unauthorizedUserIsBlocked() {
        securityService.addAuthorizedUser(1);

        User user = buildUser(999L, "stranger");

        assertFalse(securityService.isAuthorizedUser(user));
    }

    @Test
    void addAuthorizedUserIsIdempotent() {
        securityService.addAuthorizedUser(7);
        securityService.addAuthorizedUser(7);

        SecurityConfigEntity config = configRepository.getSecurityConfig();
        long count = config.getAuthorizedUserList().stream().filter(id -> id == 7).count();
        assertTrue(count <= 1, "Duplicate entries for userId 7: " + count);
    }

    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUserName(username);
        user.setFirstName(username);
        user.setLastName("");
        user.setIsBot(false);
        return user;
    }
}
