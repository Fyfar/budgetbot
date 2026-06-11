package com.home.budgetbot.bank.webhook;

import com.home.budgetbot.bank.client.ClientInfoDto;
import com.home.budgetbot.bank.client.MonobankClient;
import com.home.budgetbot.bank.client.SetWebhookRequest;
import com.home.budgetbot.bank.config.MonobankProperties;
import com.home.budgetbot.bank.config.MonobankSecretProperties;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
@Singleton
public class WebhookRegistrar {

    @Inject
    MonobankClient monobankClient;

    @Inject
    MonobankSecretProperties secretProperties;

    @Inject
    MonobankProperties monobankProperties;

    @EventListener
    public void onStartup(StartupEvent event) {
        register();
    }

    // Monobank disables the webhook after 3 failed deliveries; re-assert every 6 hours.
    @Scheduled(cron = "0 0 */6 * * *")
    public void ensureRegistered() {
        String expected = expectedUrl();
        if (expected == null) {
            return;
        }
        for (String token : secretProperties.getTokenList()) {
            try {
                ClientInfoDto info = monobankClient.getClientInfo(token);
                if (!Objects.equals(expected, info.getWebHookUrl())) {
                    log.warn("Webhook drift detected for token {}***; re-registering",
                            token.substring(0, Math.min(4, token.length())));
                    monobankClient.setWebhook(token, new SetWebhookRequest(expected));
                }
            } catch (Exception e) {
                log.error("Webhook drift check failed", e);
            }
        }
    }

    private void register() {
        String expected = expectedUrl();
        if (expected == null) {
            log.info("monobank.webhook-public-url not set; skipping webhook registration");
            return;
        }
        for (String token : secretProperties.getTokenList()) {
            try {
                monobankClient.setWebhook(token, new SetWebhookRequest(expected));
                log.info("Registered webhook for token {}***",
                        token.substring(0, Math.min(4, token.length())));
            } catch (Exception e) {
                log.error("Failed to register webhook for token", e);
            }
        }
    }

    private String expectedUrl() {
        String base = monobankProperties.getWebhookPublicUrl();
        String secret = monobankProperties.getWebhookSecret();
        if (base == null || base.isBlank() || secret == null || secret.isBlank()) {
            return null;
        }
        String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return b + "/personal/balance/webhook/" + secret;
    }
}
