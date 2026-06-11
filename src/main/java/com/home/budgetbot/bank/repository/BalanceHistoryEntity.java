package com.home.budgetbot.bank.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

import java.time.OffsetDateTime;

@Entity
@Data
@ToString
@Table(name = "balance_history")
public class BalanceHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid")
    private String id;

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
