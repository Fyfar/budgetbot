package com.home.budgetbot.bank.repository;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;

@JdbcRepository(dialect = Dialect.H2)
@Replaces(BalanceHistoryRepository.class)
public interface TestBalanceHistoryRepository extends BalanceHistoryRepository {
    void deleteAll();
}
