package com.home.budgetbot.bot.repository.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.experimental.Accessors;

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
