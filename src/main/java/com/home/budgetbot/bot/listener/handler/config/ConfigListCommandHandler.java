package com.home.budgetbot.bot.listener.handler.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.home.budgetbot.bot.listener.handler.AbstractCommandHandler;
import com.home.budgetbot.bot.listener.handler.UpdateWrapper;
import com.home.budgetbot.bot.service.ConfigService;
import com.home.budgetbot.bot.service.model.ConfigModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.SneakyThrows;

@Singleton
public class ConfigListCommandHandler extends AbstractCommandHandler {
    public static final String COMMAND = "/config";

    @Inject
    ConfigService configService;

    @Inject
    ObjectMapper objectMapper;

    public ConfigListCommandHandler() {
        super(COMMAND, "Настройки в JSON формате");
    }

    @Override
    @SneakyThrows
    public void handle(UpdateWrapper wrapper) {
        ConfigModel config = configService.getConfig();
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);

        sendResponse(json);
    }
}
