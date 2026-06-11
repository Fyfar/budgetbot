package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.ConfigRepository;
import com.home.budgetbot.bot.repository.entity.config.SecurityConfigEntity;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Singleton
public class SecurityService {

    @Inject
    ConfigRepository configRepository;

    public boolean isAuthorizedUser(User user) {
        SecurityConfigEntity securityConfig = configRepository.getSecurityConfig();

        if (isAdminNotExist(securityConfig)) {
            log.warn("Admin not exist, save {} as admin", user.getUserName());
            addAuthorizedUser(user.getId().intValue());
            return true;
        }

        List<Integer> userIdList = new ArrayList<>(securityConfig.getAuthorizedUserList());

        return userIdList.contains(user.getId().intValue());
    }

    public void addAuthorizedUser(Integer userId) {
        SecurityConfigEntity securityConfig = configRepository.getSecurityConfig();

        securityConfig.getAuthorizedUserList().add(userId);

        configRepository.update(securityConfig);
    }

    private boolean isAdminNotExist(SecurityConfigEntity securityConfig) {
        return Optional.ofNullable(securityConfig)
                .map(SecurityConfigEntity::getAuthorizedUserList)
                .filter(list -> !list.isEmpty())
                .isEmpty();
    }
}
