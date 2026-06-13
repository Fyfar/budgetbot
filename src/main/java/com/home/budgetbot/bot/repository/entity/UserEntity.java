package com.home.budgetbot.bot.repository.entity;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@MappedEntity("user_entity")
public class UserEntity {
    @Id
    private int id;
    private String username;
    private String firstName;
    private String lastName;
    private Long chatId;
}
