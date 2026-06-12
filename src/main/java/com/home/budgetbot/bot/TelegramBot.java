package com.home.budgetbot.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.Consumer;

public class TelegramBot extends TelegramLongPollingBot {
    private final String botUsername;
    private Consumer<Update> updateConsumer = update -> {
    };

    public TelegramBot(String botUsername, String botToken) {
        super(botToken);
        this.botUsername = botUsername;
    }

    public void setUpdateListener(Consumer<Update> listener) {
        updateConsumer = listener;
    }

    @Override
    public void onUpdateReceived(Update update) {
        updateConsumer.accept(update);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}
