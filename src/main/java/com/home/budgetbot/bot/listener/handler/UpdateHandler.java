package com.home.budgetbot.bot.listener.handler;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface UpdateHandler {
    boolean isSupport(Update update);

    void handle(Update update);
}
