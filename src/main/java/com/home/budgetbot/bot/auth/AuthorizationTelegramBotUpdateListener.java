package com.home.budgetbot.bot.auth;

import com.home.budgetbot.bot.listener.TelegramBotUpdateEvent;
import com.home.budgetbot.bot.service.SecurityService;
import com.home.budgetbot.bot.service.UserService;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Slf4j
@Singleton
public class AuthorizationTelegramBotUpdateListener {

    @Inject
    ApplicationEventPublisher<TelegramBotUpdateEvent> eventPublisher;

    @Inject
    SecurityService securityService;

    @Inject
    UserService userService;

    @EventListener
    public void onTelegramBotUpdate(Update update) {
        User user = AbilityUtils.getUser(update);
        Long chatId = AbilityUtils.getChatId(update);

        log.info("Receive update with id: {} from user: {}", update.getUpdateId(), user.getUserName());

        if (securityService.isAuthorizedUser(user)) {
            if (userService.isNotExist(user.getId().intValue())) {
                log.info("Save new user {} {} with username: {}", user.getFirstName(), user.getLastName(), user.getUserName());
                userService.save(user.getId().intValue(), user.getUserName(), user.getFirstName(), user.getLastName(), chatId);
            }

            TelegramBotUpdateEvent event = new TelegramBotUpdateEvent(update, user.getUserName(), chatId);
            eventPublisher.publishEvent(event);
        } else {
            log.warn("Unauthorized message from {} with chat id: {}", user.getUserName(), chatId);
        }
    }
}
