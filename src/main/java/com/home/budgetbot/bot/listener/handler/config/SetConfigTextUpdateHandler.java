package com.home.budgetbot.bot.listener.handler.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.home.budgetbot.bot.listener.handler.AbstractUpdateWrapperHandler;
import com.home.budgetbot.bot.listener.handler.UpdateWrapper;
import com.home.budgetbot.bot.service.ConfigService;
import com.home.budgetbot.bot.service.model.ConfigModel;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Log4j2
@Component
public class SetConfigTextUpdateHandler extends AbstractUpdateWrapperHandler {

    private List<String> userList = new CopyOnWriteArrayList<>();

    @Autowired
    private ConfigService configService;

    @Autowired
    private ObjectMapper objectMapper;

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
                sendResponse("Error while parse message: "+exception.getMessage());
            }
        });
    }

    public void nextMessageIsConfig(String user) {
        userList.add(user);
    }
}
