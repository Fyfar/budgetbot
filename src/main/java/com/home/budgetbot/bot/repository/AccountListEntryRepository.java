package com.home.budgetbot.bot.repository;

import com.home.budgetbot.bot.repository.entity.config.AccountListEntry;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

@JdbcRepository(dialect = Dialect.H2)
public interface AccountListEntryRepository extends CrudRepository<AccountListEntry, Long> {
    void deleteByBudgetConfigId(Long budgetConfigId);
}
