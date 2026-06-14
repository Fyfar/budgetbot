package com.home.budgetbot.bot.repository.entity.config;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import lombok.Data;

@Data
@MappedEntity("budget_account")
public class AccountListEntry {
    @Id
    @GeneratedValue
    private Long id;

    private Long budgetConfigId;
    private String account;

    public AccountListEntry() {}

    public AccountListEntry(Long budgetConfigId, String account) {
        this.budgetConfigId = budgetConfigId;
        this.account = account;
    }
}
