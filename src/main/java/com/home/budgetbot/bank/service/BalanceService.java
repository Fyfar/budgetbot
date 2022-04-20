package com.home.budgetbot.bank.service;

import com.home.budgetbot.bank.model.BalanceChangedWebhookInput;

public interface BalanceService {

    void balanceChanged(BalanceChangedWebhookInput input);

}
