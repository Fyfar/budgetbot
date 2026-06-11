package com.home.budgetbot.bot.listener;


import com.home.budgetbot.bot.listener.handler.UpdateHandler;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Slf4j
@Singleton
public class TelegramBotUpdateListener {

    @Inject
    TelegramBotUpdateEventHolder eventHolder;

    @Inject
    List<UpdateHandler> updateHandlerList;

    @EventListener
    public void handleTelegramUpdate(TelegramBotUpdateEvent event) {
        Update update = event.getUpdate();

        try {
            eventHolder.set(event);

            for (UpdateHandler updateHandler : updateHandlerList) {
                if (updateHandler.isSupport(update)) {
                    log.info("Handle request by: {}", updateHandler.getClass().getSimpleName());
                    updateHandler.handle(update);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error while handle update", e);
        } finally {
            eventHolder.clean();
        }
    }
}
