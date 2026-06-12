package com.home.budgetbot.bank.webhook;

import com.home.budgetbot.bank.config.MonobankProperties;
import com.home.budgetbot.bank.model.BalanceChangedWebhookInput;
import com.home.budgetbot.bank.service.BalanceService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

@Slf4j
@Controller("/personal/balance/webhook")
public class WebhookController {

    @Inject
    BalanceService balanceService;

    @Inject
    MonobankProperties monobankProperties;

    @Inject
    @Named(TaskExecutors.IO)
    ExecutorService ioExecutor;

    // Monobank validates the URL with a GET before delivering events; must return 200.
    @Get("/{secret}")
    public HttpResponse<?> validate(@PathVariable String secret) {
        if (!secretMatches(secret)) {
            return HttpResponse.notFound();
        }
        return HttpResponse.ok();
    }

    @Post(value = "/{secret}", consumes = MediaType.APPLICATION_JSON)
    public HttpResponse<?> balanceChangedWebhook(@PathVariable String secret,
                                                 @Body BalanceChangedWebhookInput input) {
        if (!secretMatches(secret)) {
            return HttpResponse.notFound();
        }
        ioExecutor.submit(() -> balanceService.balanceChanged(input));
        return HttpResponse.ok();
    }

    private boolean secretMatches(String secret) {
        String expected = monobankProperties.getWebhookSecret();
        return expected != null && !expected.isBlank() && Objects.equals(secret, expected);
    }
}
