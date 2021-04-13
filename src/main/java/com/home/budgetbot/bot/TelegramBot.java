package com.home.budgetbot.bot;

import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.Consumer;

@Log4j2
public class TelegramBot extends TelegramLongPollingBot {
    private String botUsername;
    private String botToken;
    private Consumer<Update> updateConsumer = update -> {};

    public TelegramBot(String botUsername, String botToken) {
        this.botUsername = botUsername;
        this.botToken = botToken;
    }

    public void setUpdateListener(Consumer<Update> listener) {
        updateConsumer = listener;
    }

    @Override
    public void onUpdateReceived(Update update) {
        updateConsumer.accept(update);
    }

    @Override
    public void clearWebhook() {
        log.warn("Call clear webhook");
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onRegister() {
        log.info("Bot register complete");
    }
}
