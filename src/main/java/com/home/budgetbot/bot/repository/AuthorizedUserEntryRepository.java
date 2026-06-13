package com.home.budgetbot.bot.repository;

import com.home.budgetbot.bot.repository.entity.config.AuthorizedUserEntry;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

@JdbcRepository(dialect = Dialect.H2)
public interface AuthorizedUserEntryRepository extends CrudRepository<AuthorizedUserEntry, Long> {
    void deleteBySecurityConfigId(Long securityConfigId);

    boolean existsBySecurityConfigIdAndUserId(Long securityConfigId, Integer userId);
}
