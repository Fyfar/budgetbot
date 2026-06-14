package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.AuthorizedUserEntryRepository;
import com.home.budgetbot.bot.repository.SecurityConfigRepository;
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
    SecurityConfigRepository securityConfigRepository;

    @Inject
    AuthorizedUserEntryRepository authorizedUserEntryRepository;

    @BeforeEach
    void clearAuthorizedUsers() {
        SecurityConfigEntity config = securityConfigRepository.findAll().iterator().next();
        authorizedUserEntryRepository.deleteBySecurityConfigId(config.getId());
    }

    @Test
    void firstUserBecomesAdmin() {
        User user = buildUser(42L, "firstUser");

        boolean result = securityService.isAuthorizedUser(user);

        assertTrue(result);
        SecurityConfigEntity config = securityConfigRepository.findAll().iterator().next();
        assertTrue(config.getAuthorizedUserList().stream().anyMatch(e -> e.getUserId() == 42));
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

        SecurityConfigEntity config = securityConfigRepository.findAll().iterator().next();
        long count = config.getAuthorizedUserList().stream().filter(e -> e.getUserId() == 7).count();
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
