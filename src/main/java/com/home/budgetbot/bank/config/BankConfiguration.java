package com.home.budgetbot.bank.config;

import com.home.budgetbot.common.PropertyProvider;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class BankConfiguration {

    @Singleton
    public MonobankSecretProperties monobankSecretProperties(PropertyProvider propertyProvider) {
        return propertyProvider.getPropertySupplier("monobank", MonobankSecretProperties.class);
    }
}
