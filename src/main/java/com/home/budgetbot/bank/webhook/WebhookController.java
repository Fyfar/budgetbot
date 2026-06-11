package com.home.budgetbot.bank.webhook;

import com.home.budgetbot.bank.model.BalanceChangedWebhookInput;
import com.home.budgetbot.bank.service.BalanceService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Inject;

@Controller("/personal/balance/webhook")
public class WebhookController {

    @Inject
    BalanceService balanceService;

    @Post(consumes = MediaType.APPLICATION_JSON)
    public HttpResponse<Void> balanceChangedWebhook(@Body BalanceChangedWebhookInput input) {
        balanceService.balanceChanged(input);
        return HttpResponse.noContent();
    }

}
