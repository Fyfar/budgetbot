package com.home.budgetbot.bot.repository;

import com.home.budgetbot.bot.repository.entity.config.BudgetConfigEntity;
import com.home.budgetbot.bot.repository.entity.config.ConfigEntity;
import com.home.budgetbot.bot.repository.entity.config.ConfigType;
import com.home.budgetbot.bot.repository.entity.config.SecurityConfigEntity;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.GenericRepository;

@Repository
public interface ConfigRepository extends GenericRepository<ConfigEntity, ConfigType> {

    <S extends ConfigEntity> S update(S entity);

    @Nullable
    @Query("SELECT config FROM BudgetConfigEntity config")
    BudgetConfigEntity getBudgetConfig();

    @Nullable
    @Query("SELECT config FROM SecurityConfigEntity config")
    SecurityConfigEntity getSecurityConfig();
}
