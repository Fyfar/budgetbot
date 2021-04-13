package com.home.budgetbot.bot.repository;

import com.home.budgetbot.bot.repository.entity.config.BudgetConfigEntity;
import com.home.budgetbot.bot.repository.entity.config.ConfigEntity;
import com.home.budgetbot.bot.repository.entity.config.ConfigType;
import com.home.budgetbot.bot.repository.entity.config.SecurityConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigRepository extends JpaRepository<ConfigEntity, ConfigType> {

    @Query("SELECT config FROM BudgetConfigEntity config WHERE config.type = 'BUDGET'")
    BudgetConfigEntity getBudgetConfig();

    @Query("SELECT config FROM SecurityConfigEntity config WHERE config.type = 'SECURITY'")
    SecurityConfigEntity getSecurityConfig();
}
