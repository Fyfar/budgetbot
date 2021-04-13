package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.UserRepository;
import com.home.budgetbot.bot.repository.entity.UserEntity;
import com.home.budgetbot.bot.service.model.InlineKeyboardModel;
import com.home.budgetbot.bot.service.model.MessageModel;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Log4j2
@Service
public class MessageService {

    @Autowired
    private TelegramLongPollingBot telegramBot;

    @Autowired
    private UserRepository userRepository;

    public void notifyAll(MessageModel messageModel) {
        List<String> chatList = userRepository.findAll().stream()
                .map(UserEntity::getChatId)
                .map(String::valueOf)
                .collect(Collectors.toList());

        messageModel.setChatList(chatList);

        sendMessage(messageModel);
    }

    public void sendMessage(MessageModel payload) {
        if (payload.getImageList().size() == 1) {
            prepareSendPhoto(payload).forEach(sendPhoto -> {
                try {
                    telegramBot.execute(sendPhoto);
                } catch (TelegramApiException exception) {
                    log.error("Error while send message", exception);
                }
            });
        } else if (payload.getImageList().size() > 1) {
            prepare(payload).forEach(sendMediaGroup -> {
                try {
                    telegramBot.execute(sendMediaGroup);
                } catch (TelegramApiException exception) {
                    log.error("Error while send message", exception);
                }
            });
        } else {
            prepareSimpleMessage(payload).forEach(sendMessage -> {
                try {
                    telegramBot.execute(sendMessage);
                } catch (TelegramApiException exception) {
                    log.error("Error while send message", exception);
                }
            });
        }
    }

    private List<SendMediaGroup> prepare(MessageModel payload) {
        return payload.getChatList().stream()
                .map(chatId -> {
                    List<File> imageFiles = replaceFolderToFilePath(payload.getImageList());

                    List<InputMedia> inputMedia = new ArrayList<>();
                    for (File imageFile : imageFiles) {
                        InputMediaPhoto inputMediaPhoto = new InputMediaPhoto();
                        inputMediaPhoto.setMedia(imageFile, UUID.randomUUID().toString());
                        inputMedia.add(inputMediaPhoto);
                    }

                    SendMediaGroup group = new SendMediaGroup();
                    group.setMedias(inputMedia);
                    group.setChatId(chatId);

                    return group;
                })
                .collect(Collectors.toList());
    }

    private List<SendPhoto> prepareSendPhoto(MessageModel payload) {
        return payload.getChatList().stream()
                .map(chatId -> {
                    List<File> imageFiles = replaceFolderToFilePath(payload.getImageList());

                    SendPhoto sendPhoto = new SendPhoto();
                    sendPhoto.setCaption(payload.getMessage());
                    sendPhoto.setPhoto(new InputFile(imageFiles.get(0)));
                    sendPhoto.setChatId(chatId);

                    return sendPhoto;
                })
                .collect(Collectors.toList());
    }

    private List<SendMessage> prepareSimpleMessage(MessageModel payload) {
        return payload.getChatList().stream()
                .map(chatId -> {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText(payload.getMessage());

                    if (payload.getInlineKeyboard().size() > 0) {
                        InlineKeyboardMarkup keyboard = buildInlineKeyboard(payload.getInlineKeyboard());
                        sendMessage.setReplyMarkup(keyboard);
                    }

                    return sendMessage;
                })
                .collect(Collectors.toList());
    }

    private InlineKeyboardMarkup buildInlineKeyboard(List<List<InlineKeyboardModel>> inlineKeyboard) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();

        for (List<InlineKeyboardModel> rowBlueprint : inlineKeyboard) {
            List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
            for (InlineKeyboardModel buttonBlueprint : rowBlueprint) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(buttonBlueprint.getText());
                button.setCallbackData(buttonBlueprint.getData());
                keyboardRow.add(button);
            }
            builder.keyboardRow(keyboardRow);
        }

        return builder.build();
    }

    private List<File> replaceFolderToFilePath(List<String> imagePathList) {
        List<File> result = new ArrayList<>();

        for (String path : imagePathList) {
            File file = new File(path);
            if (file.isDirectory()) {
                List<File> files = Arrays.stream(file.listFiles())
                        .filter(File::isFile)
                        .collect(Collectors.toList());
                result.addAll(files);
            } else {
                result.add(file);
            }
        }

        return result;
    }

    public void completeCallbackQuery(String callbackQueryId) {
        try {
            telegramBot.execute(new AnswerCallbackQuery(callbackQueryId));
        } catch (TelegramApiException e) {
            log.error("Error while complete callback query");
        }
    }
}
