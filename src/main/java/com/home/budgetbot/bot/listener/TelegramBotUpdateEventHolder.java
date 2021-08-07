package com.home.budgetbot.bot.listener;

import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
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
