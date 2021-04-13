package com.home.budgetbot.bank.service;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class BalanceHistoryModel {
    private Integer balance;
    private OffsetDateTime time;
}
