package com.home.budgetbot.bot.repository;

import com.home.budgetbot.bot.repository.entity.UserEntity;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

/**
 * CrudRepository supplies save / findById / findAll / count out of the box —
 * no @Query needed for these standard operations.
 */
@JdbcRepository(dialect = Dialect.H2)
public interface UserRepository extends CrudRepository<UserEntity, Integer> {
}
