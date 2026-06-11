package com.home.budgetbot.bot.listener;

import jakarta.inject.Singleton;

import java.util.function.Supplier;

@Singleton
public class TelegramBotUpdateEventHolder implements Supplier<TelegramBotUpdateEvent> {

    private final ThreadLocal<TelegramBotUpdateEvent> threadLocal = new ThreadLocal<>();

    void set(TelegramBotUpdateEvent context) {
        threadLocal.set(context);
    }

    void clean() {
        threadLocal.remove();
    }

    @Override
    public TelegramBotUpdateEvent get() {
        return threadLocal.get();
    }
}
