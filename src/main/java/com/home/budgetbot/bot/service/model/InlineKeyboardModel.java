package com.home.budgetbot.bot.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InlineKeyboardModel {
    private String text;
    private String data;
}
