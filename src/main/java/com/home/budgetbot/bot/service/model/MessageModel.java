package com.home.budgetbot.bot.service.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@Accessors(chain = true)
public class MessageModel {
    private String message;
    private List<String> chatList = new ArrayList<>();
    private List<String> imageList = new ArrayList<>();
    private List<List<InlineKeyboardModel>> inlineKeyboard = new ArrayList<>();

    public MessageModel() {
    }

    public MessageModel(String chatId, String message) {
        this.chatList = Collections.singletonList(chatId);
        this.message = message;
    }

    public MessageModel addImage(String filePath) {
        imageList.add(filePath);
        return this;
    }
}
