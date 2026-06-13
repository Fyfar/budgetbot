package com.home.budgetbot.bot.repository.entity.config;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import lombok.Data;

@Data
@MappedEntity("security_user")
public class AuthorizedUserEntry {
    @Id
    @GeneratedValue
    private Long id;

    private Long securityConfigId;
    private Integer userId;

    public AuthorizedUserEntry() {}

    public AuthorizedUserEntry(Long securityConfigId, Integer userId) {
        this.securityConfigId = securityConfigId;
        this.userId = userId;
    }
}
