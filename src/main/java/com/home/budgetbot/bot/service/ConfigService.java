package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.ConfigRepository;
import com.home.budgetbot.bot.repository.entity.config.BudgetConfigEntity;
import com.home.budgetbot.bot.repository.entity.config.SecurityConfigEntity;
import com.home.budgetbot.bot.service.mapper.BudgetConfigMapper;
import com.home.budgetbot.bot.service.mapper.SecurityConfigMapper;
import com.home.budgetbot.bot.service.model.BudgetConfigModel;
import com.home.budgetbot.bot.service.model.ConfigModel;
import com.home.budgetbot.bot.service.model.SecurityConfigModel;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Log4j2
@Service
public class ConfigService implements InitializingBean {

    @Autowired
    public ConfigRepository configRepository;

    @Autowired
    public BudgetConfigMapper budgetConfigMapper;

    @Autowired
    public SecurityConfigMapper securityConfigMapper;

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
        configRepository.save(budgetEntity);

        SecurityConfigEntity securityConfigEntity = securityConfigMapper.map(config.getSecurity());
        configRepository.save(securityConfigEntity);
    }

    @Override
    public void afterPropertiesSet() {
        if (configRepository.getBudgetConfig() == null) {
            log.info("Budget config not found. Init default values.");

            BudgetConfigEntity config = new BudgetConfigEntity()
                    .setSalaryDay(5)
                    .setAccountList(Arrays.asList("remove_me_example_id"))
                    .setBudgetLimit(900);

            configRepository.save(config);
        }

        if (configRepository.getSecurityConfig() == null) {
            log.info("Security config not found. Init default values.");

            SecurityConfigEntity securityConfigEntity = new SecurityConfigEntity();

            configRepository.save(securityConfigEntity);
        }
    }
}
