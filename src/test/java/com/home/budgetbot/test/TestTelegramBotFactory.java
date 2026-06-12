package com.home.budgetbot.test;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import static org.mockito.Mockito.mock;

/**
 * Provides a mock Telegram bot bean when the real bot is disabled (test env),
 * so beans that depend on it (e.g. MessageService) remain creatable.
 */
@Factory
@Requires(env = "disableTelegramBot")
public class TestTelegramBotFactory {

    @Singleton
    TelegramLongPollingBot telegramBot() {
        return mock(TelegramLongPollingBot.class);
    }
}
