package com.home.budgetbot.bank.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface BankService {
    Optional<Integer> findLastBalance(String accountId);

    List<BalanceHistoryModel> findBalanceHistoryByDay(String accountId, OffsetDateTime dateTime);

    Optional<Integer> findInitialBalanceByDay(String accountId, OffsetDateTime dateTime);

    Optional<Integer> findBalanceDifferenceByDay(String accountId, OffsetDateTime dateTime);

    void saveToHistory(String accountId, int balance, int penny);
}
