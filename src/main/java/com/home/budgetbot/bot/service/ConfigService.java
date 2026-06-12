package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.ConfigRepository;
import com.home.budgetbot.bot.repository.entity.config.BudgetConfigEntity;
import com.home.budgetbot.bot.repository.entity.config.SecurityConfigEntity;
import com.home.budgetbot.bot.service.mapper.BudgetConfigMapper;
import com.home.budgetbot.bot.service.mapper.SecurityConfigMapper;
import com.home.budgetbot.bot.service.model.BudgetConfigModel;
import com.home.budgetbot.bot.service.model.ConfigModel;
import com.home.budgetbot.bot.service.model.SecurityConfigModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ConfigService {

    @Inject
    ConfigRepository configRepository;

    @Inject
    BudgetConfigMapper budgetConfigMapper;

    @Inject
    SecurityConfigMapper securityConfigMapper;

    public ConfigModel getConfig() {
        BudgetConfigEntity budgetConfig = configRepository.getBudgetConfig();
        BudgetConfigModel budgetConfigModel = budgetConfigMapper.map(budgetConfig);

        SecurityConfigEntity securityConfig = configRepository.getSecurityConfig();
        SecurityConfigModel securityConfigModel = securityConfigMapper.map(securityConfig);

        return new ConfigModel()
                .setBudget(budgetConfigModel)
                .setSecurity(securityConfigModel);
    }

    public void setConfig(ConfigModel config) {
        BudgetConfigEntity budgetEntity = budgetConfigMapper.map(config.getBudget());
        configRepository.update(budgetEntity);

        SecurityConfigEntity securityConfigEntity = securityConfigMapper.map(config.getSecurity());
        configRepository.update(securityConfigEntity);
    }
}
