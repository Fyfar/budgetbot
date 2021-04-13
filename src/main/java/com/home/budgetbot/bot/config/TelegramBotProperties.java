package com.home.budgetbot.bot.config;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class TelegramBotProperties {
    private String login;
    private String token;
}
