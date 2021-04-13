package com.home.budgetbot.bank.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.home.budgetbot.bank.client.MonobankClient;
import com.home.budgetbot.common.PropertyProvider;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BankConfiguration {

    @Bean
    public MonobankClient monobankClient(MonobankProperties monobankProperties) {
        return Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .target(MonobankClient.class, monobankProperties.getBaseUrl());
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public MonobankSecretProperties monobankSecretProperties(PropertyProvider propertyProvider) {
        return propertyProvider.getPropertySupplier("monobank", MonobankSecretProperties.class);
    }
}
