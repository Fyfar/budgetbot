package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.AuthorizedUserEntryRepository;
import com.home.budgetbot.bot.repository.SecurityConfigRepository;
import com.home.budgetbot.bot.repository.entity.config.AuthorizedUserEntry;
import com.home.budgetbot.bot.repository.entity.config.SecurityConfigEntity;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;
import java.util.Optional;

@Slf4j
@Singleton
public class SecurityService {

    @Inject
    SecurityConfigRepository securityConfigRepository;

    @Inject
    AuthorizedUserEntryRepository authorizedUserEntryRepository;

    public synchronized boolean isAuthorizedUser(User user) {
        SecurityConfigEntity securityConfig = getConfig();

        if (isAdminNotExist(securityConfig)) {
            log.warn("Admin not exist, save {} as admin", user.getUserName());
            addAuthorizedUser(user.getId().intValue());
            return true;
        }

        return securityConfig.getAuthorizedUserList().stream()
                .anyMatch(e -> e.getUserId() != null && e.getUserId() == user.getId().intValue());
    }

    public synchronized void addAuthorizedUser(Integer userId) {
        SecurityConfigEntity securityConfig = getConfig();

        if (!authorizedUserEntryRepository.existsBySecurityConfigIdAndUserId(securityConfig.getId(), userId)) {
            authorizedUserEntryRepository.save(new AuthorizedUserEntry(securityConfig.getId(), userId));
        }
    }

    private SecurityConfigEntity getConfig() {
        List<SecurityConfigEntity> all = securityConfigRepository.findAll();
        if (all.isEmpty()) throw new IllegalStateException("Security config not initialized");
        return all.get(0);
    }

    private boolean isAdminNotExist(SecurityConfigEntity securityConfig) {
        return Optional.ofNullable(securityConfig)
                .map(SecurityConfigEntity::getAuthorizedUserList)
                .filter(list -> !list.isEmpty())
                .isEmpty();
    }
}
