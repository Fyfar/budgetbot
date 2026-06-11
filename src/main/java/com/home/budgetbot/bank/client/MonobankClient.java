package com.home.budgetbot.bank.client;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.client.annotation.Client;

@Client("${monobank.base-url}")
public interface MonobankClient {

    @Get("/personal/client-info")
    ClientInfoDto getClientInfo(@Header("X-Token") String token);
}
