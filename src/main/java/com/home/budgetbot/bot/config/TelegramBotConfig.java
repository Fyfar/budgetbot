package com.home.budgetbot.bot.config;

import com.home.budgetbot.bot.TelegramBot;
import com.home.budgetbot.bot.listener.handler.AbstractCommandHandler;
import com.home.budgetbot.common.PropertyProvider;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Factory
@Requires(notEnv = "disableTelegramBot")
public class TelegramBotConfig {

    @Singleton
    public TelegramBotProperties telegramBotProperties(PropertyProvider propertyProvider) {
        return propertyProvider.getPropertySupplier("telegram", TelegramBotProperties.class);
    }

    @Singleton
    public TelegramLongPollingBot telegramBot(TelegramBotProperties properties,
                                              ApplicationEventPublisher<Update> eventPublisher) {
        if (properties.getToken() == null) {
            throw new IllegalArgumentException("Bot token not found");
        }

        TelegramBot telegramBot = new TelegramBot(properties.getLogin(), properties.getToken());
        telegramBot.setUpdateListener(eventPublisher::publishEvent);

        return telegramBot;
    }

    @Context
    public TelegramBotsApi botsApi(TelegramLongPollingBot bot, List<AbstractCommandHandler> commandHandlers) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(bot);

            List<BotCommand> commandList = commandHandlers.stream()
                    .filter(command -> !command.isHideCommand())
                    .map(command -> new BotCommand(command.getCommand(), command.getDescription()))
                    .collect(Collectors.toList());

            bot.execute(SetMyCommands.builder().commands(commandList).build());

            return telegramBotsApi;
        } catch (TelegramApiException e) {
            throw new IllegalStateException("Error while create telegram bot API", e);
        }
    }
}
