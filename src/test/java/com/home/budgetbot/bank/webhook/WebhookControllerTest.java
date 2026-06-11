package com.home.budgetbot.bank.webhook;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest(environments = {"integration", "disableTelegramBot"})
class WebhookControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void getWithValidSecretReturns200() {
        HttpStatus status = client.toBlocking()
                .exchange(HttpRequest.GET("/personal/balance/webhook/test-secret"))
                .status();
        assertEquals(HttpStatus.OK, status);
    }

    @Test
    void getWithWrongSecretReturns404() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.GET("/personal/balance/webhook/wrong")));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
