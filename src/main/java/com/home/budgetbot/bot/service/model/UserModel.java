package com.home.budgetbot.bot.service.model;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@ToString
public class UserModel {
    private int id;
    private String firstName;
    private String lastName;
    private Long chatId;
}
