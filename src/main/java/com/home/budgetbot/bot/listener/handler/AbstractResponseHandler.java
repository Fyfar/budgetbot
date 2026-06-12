package com.home.budgetbot.bot.listener.handler;

import com.home.budgetbot.bot.listener.TelegramBotUpdateEvent;
import com.home.budgetbot.bot.listener.TelegramBotUpdateEventHolder;
import com.home.budgetbot.bot.service.MessageService;
import com.home.budgetbot.bot.service.model.MessageModel;
import jakarta.inject.Inject;

public abstract class AbstractResponseHandler implements UpdateHandler {
    @Inject
    protected MessageService messageService;

    @Inject
    protected TelegramBotUpdateEventHolder eventHolder;

    protected void sendResponse(String message) {
        messageService.sendMessage(new MessageModel(getChatId(), message));
    }

    protected String getUser() {
        TelegramBotUpdateEvent event = eventHolder.get();
        return event.getUsername();
    }

    protected String getChatId() {
        TelegramBotUpdateEvent event = eventHolder.get();
        return String.valueOf(event.getChatId());
    }
}
