package com.home.budgetbot.bot.listener.handler.config;

import com.home.budgetbot.bot.listener.handler.AbstractCommandHandler;
import com.home.budgetbot.bot.listener.handler.UpdateWrapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class SetConfigCommandHandler extends AbstractCommandHandler {
    public static final String COMMAND = "/set_config";

    @Inject
    SetConfigTextUpdateHandler handler;

    public SetConfigCommandHandler() {
        super(COMMAND, "Сохранить настройки");
    }

    @Override
    public void handle(UpdateWrapper wrapper) {
        handler.nextMessageIsConfig(getUser());
        sendResponse("Жду настройки в JSON формате");
    }
}
