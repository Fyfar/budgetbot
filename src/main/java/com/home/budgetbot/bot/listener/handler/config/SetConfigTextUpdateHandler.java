package com.home.budgetbot.bot.listener.handler.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.home.budgetbot.bot.listener.handler.AbstractUpdateWrapperHandler;
import com.home.budgetbot.bot.listener.handler.UpdateWrapper;
import com.home.budgetbot.bot.service.ConfigService;
import com.home.budgetbot.bot.service.model.ConfigModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Singleton
public class SetConfigTextUpdateHandler extends AbstractUpdateWrapperHandler {

    private final List<String> userList = new CopyOnWriteArrayList<>();

    @Inject
    ConfigService configService;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public boolean isSupport(UpdateWrapper wrapper) {
        return userList.contains(getUser());
    }

    @Override
    public void handle(UpdateWrapper wrapper) {
        userList.remove(getUser());

        wrapper.getText().ifPresent(text -> {
            try {
                ConfigModel configModel = objectMapper.readValue(text, ConfigModel.class);
                configService.setConfig(configModel);
            } catch (JsonProcessingException exception) {
                log.warn("Error while parse message", exception);
                sendResponse("Error while parse message: " + exception.getMessage());
            }
        });
    }

    public void nextMessageIsConfig(String user) {
        userList.add(user);
    }
}
