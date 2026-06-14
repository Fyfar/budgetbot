package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.AccountListEntryRepository;
import com.home.budgetbot.bot.repository.BudgetConfigRepository;
import com.home.budgetbot.bot.repository.SecurityConfigRepository;
import com.home.budgetbot.bot.repository.entity.config.AccountListEntry;
import com.home.budgetbot.bot.repository.entity.config.BudgetConfigEntity;
import com.home.budgetbot.bot.repository.entity.config.SecurityConfigEntity;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds default configuration on application startup. Kept separate from
 * {@link ConfigService} so the latter stays plainly mockable in tests, and so the
 * seeding runs after the context (and its transaction support) is fully started.
 */
@Slf4j
@Singleton
public class ConfigInitializer {

    @Inject
    BudgetConfigRepository budgetConfigRepository;

    @Inject
    AccountListEntryRepository accountListEntryRepository;

    @Inject
    SecurityConfigRepository securityConfigRepository;

    @EventListener
    @Transactional
    public void onStartup(StartupEvent event) {
        if (budgetConfigRepository.count() == 0) {
            log.info("Budget config not found. Init default values.");
            BudgetConfigEntity config = new BudgetConfigEntity()
                    .setSalaryDay(5)
                    .setBudgetLimit(900);
            BudgetConfigEntity saved = budgetConfigRepository.save(config);
            accountListEntryRepository.save(new AccountListEntry(saved.getId(), "remove_me_example_id"));
        }

        if (securityConfigRepository.count() == 0) {
            log.info("Security config not found. Init default values.");
            securityConfigRepository.save(new SecurityConfigEntity());
        }
    }
}
