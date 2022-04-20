package com.home.budgetbot.bank.webhook;

import com.home.budgetbot.bank.model.BalanceChangedWebhookInput;
import com.home.budgetbot.bank.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final BalanceService balanceService;

    @RequestMapping(value = "/personal/balance/webhook", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void balanceChangedWebhook(@RequestBody BalanceChangedWebhookInput input) {
        balanceService.balanceChanged(input);
    }

}
