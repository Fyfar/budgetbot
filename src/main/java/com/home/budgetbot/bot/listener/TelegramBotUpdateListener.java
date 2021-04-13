package com.home.budgetbot.bot.listener;


import com.home.budgetbot.bot.listener.handler.AbstractCommandHandler;
import com.home.budgetbot.bot.listener.handler.UpdateHandler;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
public class TelegramBotUpdateListener implements InitializingBean {

    @Autowired
    private TelegramBotUpdateEventHolder eventHolder;

    @Autowired
    private List<UpdateHandler> updateHandlerList;

    @Autowired
    private TelegramLongPollingBot telegramBot;

    @Autowired
    private List<AbstractCommandHandler> commandHandlerList;

    @Async
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

    @Override
    public void afterPropertiesSet() throws Exception {
        List<BotCommand> commandList = this.commandHandlerList.stream()
                .filter(command -> !command.isHideCommand())
                .map(command -> new BotCommand(command.getCommand(), command.getDescription()))
                .collect(Collectors.toList());

        telegramBot.execute(new SetMyCommands(commandList));
    }
}
