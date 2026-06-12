package com.home.budgetbot.bank.client;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

@Client("${monobank.base-url}")
public interface MonobankClient {

    @Get("/personal/client-info")
    ClientInfoDto getClientInfo(@Header("X-Token") String token);

    @Post("/personal/webhook")
    void setWebhook(@Header("X-Token") String token, @Body SetWebhookRequest request);
}
