package com.home.budgetbot.bot.listener.handler.config;

import com.home.budgetbot.bot.listener.handler.AbstractUpdateWrapperHandler;
import com.home.budgetbot.bot.listener.handler.UpdateWrapper;
import com.home.budgetbot.bot.service.SecurityService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.telegram.telegrambots.meta.api.objects.Contact;

@Singleton
public class AddAuthorizedUserUpdateHandler extends AbstractUpdateWrapperHandler {

    @Inject
    SecurityService securityService;

    @Override
    public boolean isSupport(UpdateWrapper wrapper) {
        return wrapper.getContact().isPresent();
    }

    @Override
    public void handle(UpdateWrapper wrapper) {
        wrapper.getContact()
                .map(Contact::getUserId)
                .map(Long::intValue)
                .ifPresent(securityService::addAuthorizedUser);

        sendResponse("Добавил в список авторизированных пользователей");
    }
}
