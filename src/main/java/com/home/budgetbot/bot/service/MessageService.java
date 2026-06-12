package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.ConfigRepository;
import com.home.budgetbot.bot.repository.UserRepository;
import com.home.budgetbot.bot.repository.entity.UserEntity;
import com.home.budgetbot.bot.service.model.MessageModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class MessageService {

    @Inject
    TelegramLongPollingBot telegramBot;

    @Inject
    UserRepository userRepository;

    @Inject
    ConfigRepository configRepository;

    public void notifyAll(MessageModel messageModel) {
        Collection<Integer> authorizedUserList = configRepository.getSecurityConfig().getAuthorizedUserList();

        List<String> chatList = userRepository.findAll().stream()
                .map(UserEntity::getChatId)
                .filter(target -> authorizedUserList.stream().anyMatch(id -> id.longValue() == target))
                .map(String::valueOf)
                .collect(Collectors.toList());

        messageModel.setChatList(chatList);

        sendMessage(messageModel);
    }

    public void sendMessage(MessageModel payload) {
        for (String chatId : payload.getChatList()) {
            log.info("Send message to user with chat id: {}", chatId);
            try {
                telegramBot.execute(new SendMessage(chatId, payload.getMessage()));
            } catch (TelegramApiException e) {
                log.error("Error while send message", e);
            }
        }
    }

    public void completeCallbackQuery(String callbackQueryId) {
        try {
            telegramBot.execute(new AnswerCallbackQuery(callbackQueryId));
        } catch (TelegramApiException e) {
            log.error("Error while complete callback query");
        }
    }
}
