package com.home.budgetbot.bank.client;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface MonobankClient {
    @RequestLine("GET /personal/client-info")
    @Headers("X-Token: {token}")
    ClientInfoDto getClientInfo(@Param("token") String token);
}
