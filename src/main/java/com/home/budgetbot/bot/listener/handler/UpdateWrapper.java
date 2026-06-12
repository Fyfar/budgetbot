package com.home.budgetbot.bot.listener.handler;

import lombok.AllArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@AllArgsConstructor
public class UpdateWrapper {
    private final Update update;

    public Optional<String> getText() {
        Optional<String> messageText = getMessage().map(Message::getText);

        if (messageText.isPresent()) {
            return messageText;
        }

        return getCallback().map(CallbackQuery::getData);
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
