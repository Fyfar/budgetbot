package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.AccountListEntryRepository;
import com.home.budgetbot.bot.repository.AuthorizedUserEntryRepository;
import com.home.budgetbot.bot.repository.BudgetConfigRepository;
import com.home.budgetbot.bot.repository.SecurityConfigRepository;
import com.home.budgetbot.bot.repository.entity.config.AccountListEntry;
import com.home.budgetbot.bot.repository.entity.config.AuthorizedUserEntry;
import com.home.budgetbot.bot.repository.entity.config.BudgetConfigEntity;
import com.home.budgetbot.bot.repository.entity.config.SecurityConfigEntity;
import com.home.budgetbot.bot.service.mapper.BudgetConfigMapper;
import com.home.budgetbot.bot.service.mapper.SecurityConfigMapper;
import com.home.budgetbot.bot.service.model.BudgetConfigModel;
import com.home.budgetbot.bot.service.model.ConfigModel;
import com.home.budgetbot.bot.service.model.SecurityConfigModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ConfigService {

    @Inject
    BudgetConfigRepository budgetConfigRepository;

    @Inject
    AccountListEntryRepository accountListEntryRepository;

    @Inject
    SecurityConfigRepository securityConfigRepository;

    @Inject
    AuthorizedUserEntryRepository authorizedUserEntryRepository;

    @Inject
    BudgetConfigMapper budgetConfigMapper;

    @Inject
    SecurityConfigMapper securityConfigMapper;

    public ConfigModel getConfig() {
        BudgetConfigEntity budgetConfig = getFirst(budgetConfigRepository.findAll());
        BudgetConfigModel budgetConfigModel = budgetConfigMapper.map(budgetConfig);

        SecurityConfigEntity securityConfig = getFirst(securityConfigRepository.findAll());
        SecurityConfigModel securityConfigModel = securityConfigMapper.map(securityConfig);

        return new ConfigModel()
                .setBudget(budgetConfigModel)
                .setSecurity(securityConfigModel);
    }

    @Transactional
    public void setConfig(ConfigModel config) {
        BudgetConfigEntity budget = getFirst(budgetConfigRepository.findAll());
        budget.setSalaryDay(config.getBudget().getSalaryDay());
        budget.setBudgetLimit(config.getBudget().getBudgetLimit());
        budgetConfigRepository.update(budget);

        accountListEntryRepository.deleteByBudgetConfigId(budget.getId());
        for (String account : config.getBudget().getAccountList()) {
            accountListEntryRepository.save(new AccountListEntry(budget.getId(), account));
        }

        SecurityConfigEntity security = getFirst(securityConfigRepository.findAll());
        authorizedUserEntryRepository.deleteBySecurityConfigId(security.getId());
        for (Integer userId : config.getSecurity().getAuthorizedUserList()) {
            authorizedUserEntryRepository.save(new AuthorizedUserEntry(security.getId(), userId));
        }
    }

    private <T> T getFirst(Iterable<T> iterable) {
        java.util.Iterator<T> it = iterable.iterator();
        if (!it.hasNext()) throw new IllegalStateException("Config not initialized");
        return it.next();
    }
}
