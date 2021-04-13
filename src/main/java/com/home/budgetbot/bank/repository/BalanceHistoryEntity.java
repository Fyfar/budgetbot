package com.home.budgetbot.bank.repository;

import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Data
@ToString
@Table(name = "balance_history")
public class BalanceHistoryEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String uuid;

    public BalanceHistoryEntity() {
    }

    public BalanceHistoryEntity(String accountId, int balance, int penny, OffsetDateTime time) {
        this.accountId = accountId;
        this.balance = balance;
        this.time = time;
        this.penny = penny;
    }

    public BalanceHistoryEntity(String accountId, int balance, OffsetDateTime time) {
        this.accountId = accountId;
        this.balance = balance;
        this.time = time;
        this.penny = 0;
    }

    @Column(name = "account_id")
    private String accountId;
    private int balance;
    private int penny;
    private OffsetDateTime time;
}
