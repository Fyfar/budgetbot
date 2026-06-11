package com.home.budgetbot.bot.listener.handler;

import com.home.budgetbot.bot.listener.TelegramBotUpdateEvent;
import com.home.budgetbot.bot.listener.TelegramBotUpdateEventHolder;
import com.home.budgetbot.bot.service.MessageService;
import com.home.budgetbot.bot.service.model.InlineKeyboardModel;
import com.home.budgetbot.bot.service.model.MessageModel;
import jakarta.inject.Inject;

import java.util.List;

public abstract class AbstractResponseHandler implements UpdateHandler {
    @Inject
    protected MessageService messageService;

    @Inject
    protected TelegramBotUpdateEventHolder eventHolder;

    private void sendResponse(String message, List<List<InlineKeyboardModel>> inlineKeyboard) {
        MessageModel messagePayload = new MessageModel(getChatId(), message);

        if (inlineKeyboard != null) {
            messagePayload.setInlineKeyboard(inlineKeyboard);
        }

        messageService.sendMessage(messagePayload);
    }

    protected void sendResponse(String message) {
        sendResponse(message, null);
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
