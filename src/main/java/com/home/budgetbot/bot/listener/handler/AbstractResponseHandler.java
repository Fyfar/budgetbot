package com.home.budgetbot.bot.listener.handler;

import com.home.budgetbot.bot.listener.TelegramBotUpdateEvent;
import com.home.budgetbot.bot.listener.TelegramBotUpdateEventHolder;

import com.home.budgetbot.bot.service.MessageService;
import com.home.budgetbot.bot.service.model.InlineKeyboardModel;
import com.home.budgetbot.bot.service.model.MessageModel;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

public abstract class AbstractResponseHandler implements UpdateHandler {
    @Autowired
    protected MessageService messageService;

    @Autowired
    protected TelegramBotUpdateEventHolder eventHolder;

    private void sendResponse(String message, List<String> imageList, List<List<InlineKeyboardModel>> inlineKeyboard) {
        MessageModel messagePayload = new MessageModel(getChatId(), message);

        if (imageList != null) {
            messagePayload.setImageList(imageList);
        }

        if (inlineKeyboard != null) {
            messagePayload.setInlineKeyboard(inlineKeyboard);
        }

        messageService.sendMessage(messagePayload);
    }

    protected void sendResponse(String message) {
        sendResponse(message, null, null);
    }

    protected void sendResponseWithImage(String message, String image) {
        sendResponse(message, Collections.singletonList(image), null);
    }

    protected void sendResponseWithKeyboard(String message, List<List<InlineKeyboardModel>> inlineKeyboard) {
        sendResponse(message, null, inlineKeyboard);
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
