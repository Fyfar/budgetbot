package com.home.budgetbot.bank.config;

import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "monobank")
@ToString
public class MonobankProperties {
    /**
     * Base url to call API
     */
    private String baseUrl = "https://api.monobank.ua/";

    private int schedulerDelay = 70000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getSchedulerDelay() {
        return schedulerDelay;
    }

    public void setSchedulerDelay(int schedulerDelay) {
        this.schedulerDelay = schedulerDelay;
    }
}
