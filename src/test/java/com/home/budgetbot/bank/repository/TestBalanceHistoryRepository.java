package com.home.budgetbot.bank.repository;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.data.annotation.Repository;

@Repository
@Replaces(BalanceHistoryRepository.class)
public interface TestBalanceHistoryRepository extends BalanceHistoryRepository {
    void deleteAll();
}
