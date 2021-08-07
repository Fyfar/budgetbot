package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.ConfigRepository;
import com.home.budgetbot.bot.repository.UserRepository;
import com.home.budgetbot.bot.repository.entity.UserEntity;
import com.home.budgetbot.bot.service.model.MessageModel;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
public class MessageService {

    @Autowired
    private TelegramLongPollingBot telegramBot;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConfigRepository configRepository;

    public void notifyAll(MessageModel messageModel) {
        Collection<Integer> authorizedUserList = configRepository.getSecurityConfig().getAuthorizedUserList();

        List<String> chatList = userRepository.findAll().stream()
                .map(UserEntity::getChatId)
                .filter(target -> {
                    int chatId = Math.toIntExact(target);
                    return authorizedUserList.contains(chatId);
                })
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
