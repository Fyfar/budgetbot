package com.home.budgetbot.bot.repository;

import com.home.budgetbot.bot.repository.entity.config.BudgetConfigEntity;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.H2)
public interface BudgetConfigRepository extends CrudRepository<BudgetConfigEntity, Long> {
    @Join(value = "accountList", type = Join.Type.LEFT_FETCH)
    @Override
    Optional<BudgetConfigEntity> findById(Long id);

    @Join(value = "accountList", type = Join.Type.LEFT_FETCH)
    @Override
    List<BudgetConfigEntity> findAll();
}
