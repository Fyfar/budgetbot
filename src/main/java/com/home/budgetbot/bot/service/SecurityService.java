package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.ConfigRepository;
import com.home.budgetbot.bot.repository.entity.config.SecurityConfigEntity;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class SecurityService {

    @Autowired
    private ConfigRepository configRepository;

    public boolean isAuthorizedUser(User user) {
        SecurityConfigEntity securityConfig = configRepository.getSecurityConfig();

        if (isAdminNotExist(securityConfig)) {
            log.warn("Admin not exist, save {} as admin", user.getUserName());
            addAuthorizedUser(user.getId());
            return true;
        }

        List<Integer> userIdList = new ArrayList<>(securityConfig.getAuthorizedUserList());

        return userIdList.contains(user.getId());
    }

    public void addAuthorizedUser(Integer userId) {
        SecurityConfigEntity securityConfig = configRepository.getSecurityConfig();

        securityConfig.getAuthorizedUserList().add(userId);

        configRepository.save(securityConfig);
    }

    private boolean isAdminNotExist(SecurityConfigEntity securityConfig) {
        return Optional.ofNullable(securityConfig)
                .map(SecurityConfigEntity::getAuthorizedUserList)
                .filter(list -> !list.isEmpty())
                .isEmpty();
    }
}
