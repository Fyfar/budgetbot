package com.home.budgetbot.bank.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("monobank")
public class MonobankProperties {
    /**
     * Base url to call API
     */
    private String baseUrl = "https://api.monobank.ua";
}
