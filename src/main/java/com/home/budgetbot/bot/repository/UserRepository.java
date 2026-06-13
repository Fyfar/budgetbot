package com.home.budgetbot.bot.repository;

import com.home.budgetbot.bot.repository.entity.UserEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.GenericRepository;

import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.H2)
public interface UserRepository extends GenericRepository<UserEntity, Integer> {

    UserEntity save(UserEntity entity);

    List<UserEntity> findAll();

    @Query("SELECT * FROM user_entity WHERE id = :id")
    Optional<UserEntity> findById(Integer id);
}
