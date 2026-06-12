package com.home.budgetbot.bot.repository;

import com.home.budgetbot.bot.repository.entity.UserEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.GenericRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends GenericRepository<UserEntity, Integer> {

    UserEntity save(UserEntity entity);

    List<UserEntity> findAll();

    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    Optional<UserEntity> findById(Integer id);
}
