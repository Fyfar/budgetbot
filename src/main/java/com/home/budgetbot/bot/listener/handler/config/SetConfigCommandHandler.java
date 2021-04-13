package com.home.budgetbot.bot.listener.handler.config;

import com.home.budgetbot.bot.listener.handler.AbstractCommandHandler;
import com.home.budgetbot.bot.listener.handler.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SetConfigCommandHandler extends AbstractCommandHandler {
    public static final String COMMAND = "/set_config";

    @Autowired
    private SetConfigTextUpdateHandler handler;

    public SetConfigCommandHandler() {
        super(COMMAND, "Сохранить настройки");
    }

    @Override
    public void handle(UpdateWrapper wrapper) {
        handler.nextMessageIsConfig(getUser());
        sendResponse("Жду настройки в JSON формате");
    }
}
