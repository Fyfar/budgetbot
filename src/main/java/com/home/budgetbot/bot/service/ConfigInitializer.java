package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.ConfigRepository;
import com.home.budgetbot.bot.repository.entity.config.BudgetConfigEntity;
import com.home.budgetbot.bot.repository.entity.config.SecurityConfigEntity;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * Seeds default configuration on application startup. Kept separate from
 * {@link ConfigService} so the latter stays plainly mockable in tests, and so the
 * seeding runs after the context (and its transaction support) is fully started.
 */
@Slf4j
@Singleton
public class ConfigInitializer {

    @Inject
    ConfigRepository configRepository;

    @EventListener
    public void onStartup(StartupEvent event) {
        if (configRepository.getBudgetConfig() == null) {
            log.info("Budget config not found. Init default values.");

            BudgetConfigEntity config = new BudgetConfigEntity()
                    .setSalaryDay(5)
                    .setAccountList(Arrays.asList("remove_me_example_id"))
                    .setBudgetLimit(900);

            configRepository.update(config);
        }

        if (configRepository.getSecurityConfig() == null) {
            log.info("Security config not found. Init default values.");

            configRepository.update(new SecurityConfigEntity());
        }
    }
}
