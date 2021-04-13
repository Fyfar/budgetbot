package com.home.budgetbot.bot.listener.handler.config;

import com.home.budgetbot.bot.listener.handler.AbstractUpdateWrapperHandler;
import com.home.budgetbot.bot.listener.handler.UpdateWrapper;
import com.home.budgetbot.bot.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Contact;

@Component
public class AddAuthorizedUserUpdateHandler extends AbstractUpdateWrapperHandler {

    @Autowired
    private SecurityService securityService;

    @Override
    public boolean isSupport(UpdateWrapper wrapper) {
        return wrapper.getContact().isPresent();
    }

    @Override
    public void handle(UpdateWrapper wrapper) {
        wrapper.getContact()
                .map(Contact::getUserID)
                .ifPresent(securityService::addAuthorizedUser);

        sendResponse("Добавил в список авторизированных пользователей");
    }
}
