package com.home.budgetbot.bot.listener;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.Update;

@Data
@AllArgsConstructor
public class TelegramBotUpdateEvent {
    private Update update;
    private String username;
    private Long chatId;
}
