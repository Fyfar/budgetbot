package com.home.budgetbot.bank.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetWebhookRequest {
    @JsonProperty("webHookUrl")
    private String webHookUrl;
}
