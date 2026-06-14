package com.home.budgetbot.bot.repository;

import com.home.budgetbot.bot.repository.entity.config.SecurityConfigEntity;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.H2)
public interface SecurityConfigRepository extends CrudRepository<SecurityConfigEntity, Long> {
    @Join(value = "authorizedUserList", type = Join.Type.LEFT_FETCH)
    @Override
    Optional<SecurityConfigEntity> findById(Long id);

    @Join(value = "authorizedUserList", type = Join.Type.LEFT_FETCH)
    @Override
    List<SecurityConfigEntity> findAll();
}
