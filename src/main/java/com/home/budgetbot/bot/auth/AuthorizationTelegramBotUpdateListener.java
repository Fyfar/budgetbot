package com.home.budgetbot.bot.auth;

import com.home.budgetbot.bot.listener.TelegramBotUpdateEvent;
import com.home.budgetbot.bot.service.SecurityService;
import com.home.budgetbot.bot.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Log4j2
@Component
public class AuthorizationTelegramBotUpdateListener {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserService userService;

    @EventListener
    public void onTelegramBotUpdate(Update update) {
        User user = AbilityUtils.getUser(update);
        Long chatId = AbilityUtils.getChatId(update);

        log.info("Receive update with id: {} from user: {}", update.getUpdateId(), user.getUserName());

        if (securityService.isAuthorizedUser(user)) {
            if (userService.isNotExist(user.getId())) {
                log.info("Save new user {} {} with username: {}", user.getFirstName(), user.getLastName(), user.getUserName());
                userService.save(user.getId(), user.getUserName(), user.getFirstName(), user.getLastName(), chatId);
            }

            TelegramBotUpdateEvent event = new TelegramBotUpdateEvent(update, user.getUserName(), chatId);
            eventPublisher.publishEvent(event);
        } else {
            log.warn("Unauthorized message from {} with chat id: {}", user.getUserName(), chatId);
        }
    }
}
