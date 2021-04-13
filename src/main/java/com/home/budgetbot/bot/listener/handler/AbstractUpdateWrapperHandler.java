package com.home.budgetbot.bot.listener.handler;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

public abstract class AbstractUpdateWrapperHandler extends AbstractResponseHandler {
    @Override
    public boolean isSupport(Update update) {
        return Optional.of(update)
                .map(UpdateWrapper::new)
                .map(this::isSupport)
                .orElse(false);
    }

    @Override
    public void handle(Update update) {
        UpdateWrapper updateWrapper = new UpdateWrapper(update);

        try {
            this.handle(updateWrapper);
        } finally {
            updateWrapper.getCallback()
                    .map(CallbackQuery::getId)
                    .ifPresent(messageService::completeCallbackQuery);
        }
    }

    public abstract boolean isSupport(UpdateWrapper wrapper);

    public abstract void handle(UpdateWrapper wrapper);
}
