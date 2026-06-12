package com.home.budgetbot.bank.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("monobank")
public class MonobankProperties {
    private String baseUrl = "https://api.monobank.ua";
    private String webhookPublicUrl;
    private String webhookSecret;
}
