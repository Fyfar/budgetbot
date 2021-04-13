package com.home.budgetbot.bot.listener.handler;

import lombok.AllArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@AllArgsConstructor
public class UpdateWrapper {
    private Update update;

    public Optional<String> getText() {
        Optional<String> messageText = getMessage().map(Message::getText);

        if(messageText.isPresent()) {
            return messageText;
        }

        return getCallback().map(CallbackQuery::getData);
    }

    public String getTextForce() {
        return getText()
                .orElseThrow(() -> new IllegalStateException("Update don't have text"));
    }

    public Optional<Integer> getMessageId() {
        Optional<Integer> messageIdFromMessage = getMessage().map(Message::getMessageId);

        if(messageIdFromMessage.isPresent()) {
            return messageIdFromMessage;
        }

        return getCallback()
                .map(CallbackQuery::getMessage)
                .map(Message::getMessageId);
    }

    public Integer getMessageIdForce() {
        return getMessageId()
                .orElseThrow(() -> new IllegalStateException("Update don't have message id"));
    }

    public Optional<Message> getMessage() {
        return Optional.of(update)
                .filter(Update::hasMessage)
                .map(Update::getMessage);
    }

    public Optional<Contact> getContact() {
        return getMessage().map(Message::getContact);
    }

    public Optional<CallbackQuery> getCallback() {
        return Optional.of(update)
                .filter(Update::hasCallbackQuery)
                .map(Update::getCallbackQuery);
    }
}
