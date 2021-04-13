package com.home.budgetbot.bot.config;

import com.home.budgetbot.bot.TelegramBot;
import com.home.budgetbot.common.PropertyProvider;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.LongPollingBot;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Log4j2
@Configuration
public class TelegramBotConfig {

    @Bean
    public TelegramBotsApi botsApi(LongPollingBot longPollingBot) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(longPollingBot);
            return telegramBotsApi;
        } catch (TelegramApiException e) {
            throw new IllegalStateException("Error while create telegram bot API", e);
        }
    }

    @Bean
    public TelegramLongPollingBot telegramBot(TelegramBotProperties properties, ApplicationEventPublisher eventPublisher) {
        if (properties.getToken() == null) {
            throw new IllegalArgumentException("Bot token not found");
        }

        TelegramBot telegramBot = new TelegramBot(properties.getLogin(), properties.getToken());
        telegramBot.setUpdateListener(eventPublisher::publishEvent);

        return telegramBot;
    }

    @Bean
    public TelegramBotProperties applicationProperties(PropertyProvider propertyProvider) {
        return propertyProvider.getPropertySupplier("telegram", TelegramBotProperties.class);
    }
}
