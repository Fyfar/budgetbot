package com.home.budgetbot.bank.repository;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import lombok.Data;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@ToString
@MappedEntity("balance_history")
public class BalanceHistoryEntity {
    @Id
    @MappedProperty("uuid")
    private String id;

    private String accountId;
    private int balance;
    private int penny;
    private OffsetDateTime time;

    public BalanceHistoryEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public BalanceHistoryEntity(String accountId, int balance, int penny, OffsetDateTime time) {
        this();
        this.accountId = accountId;
        this.balance = balance;
        this.penny = penny;
        this.time = time;
    }

    public BalanceHistoryEntity(String accountId, int balance, OffsetDateTime time) {
        this();
        this.accountId = accountId;
        this.balance = balance;
        this.penny = 0;
        this.time = time;
    }
}
