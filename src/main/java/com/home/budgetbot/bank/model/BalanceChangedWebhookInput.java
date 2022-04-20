package com.home.budgetbot.bank.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceChangedWebhookInput {

    private String type;
    @JsonProperty("data")
    private AccountData accountData;
    @JsonProperty("statementItem")
    private BalanceChangedEvent balanceChangedEvent;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountData {
        private String account;
    }
}
