package com.home.budgetbot.bot.repository.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
@Accessors(chain = true)
public class UserEntity {
    @Id
    private int id;
    private String username;
    private String firstName;
    private String lastName;
    private Long chatId;
}
