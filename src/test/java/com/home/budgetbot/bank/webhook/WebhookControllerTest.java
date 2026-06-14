package com.home.budgetbot.bank.webhook;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.home.budgetbot.bank.BalanceChangeEventListener;
import com.home.budgetbot.bank.event.BalanceChangeEvent;
import com.home.budgetbot.bank.repository.BalanceHistoryRepository;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest(environments = {"integration", "disableTelegramBot"})
class WebhookControllerTest {

    // Real Monobank shape: statementItem is nested INSIDE data, not at the top level.
    private static final String VALID_PAYLOAD = """
            {
              "type": "StatementItem",
              "data": {
                "account": "test-account",
                "statementItem": {
                  "amount": -5000,
                  "balance": 950000,
                  "description": "coffee"
                }
              }
            }
            """;

    private static final String PING_PAYLOAD = """
            {
              "type": "StatementItem",
              "data": {"account": "test-account"}
            }
            """;

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    BalanceChangeEventListener eventListener;

    @Inject
    BalanceHistoryRepository historyRepository;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        eventListener.clean();
    }

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

    @Test
    void postWithValidSecretAndPayloadReturns200() {
        HttpStatus status = client.toBlocking()
                .exchange(HttpRequest.POST("/personal/balance/webhook/test-secret", VALID_PAYLOAD)
                        .contentType(MediaType.APPLICATION_JSON))
                .status();
        assertEquals(HttpStatus.OK, status);
    }

    @Test
    void postWithValidPayloadProcessesTransaction() {
        client.toBlocking()
                .exchange(HttpRequest.POST("/personal/balance/webhook/test-secret", VALID_PAYLOAD)
                        .contentType(MediaType.APPLICATION_JSON));

        // Processing is async — wait up to 2s for the event to land.
        await().atMost(Duration.ofSeconds(2))
                .until(() -> eventListener.getEventList().size() == 1);

        List<BalanceChangeEvent> events = eventListener.getEventList();
        assertEquals(9500, events.get(0).getNewBalance()); // 950000 kopiykas -> 9500 UAH
    }

    @Test
    void postWithWrongSecretReturns404() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST("/personal/balance/webhook/wrong", VALID_PAYLOAD)
                                .contentType(MediaType.APPLICATION_JSON)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void postWithMissingStatementItemReturns200() {
        // Monobank sends ping events without statementItem; must not return 500
        HttpStatus status = client.toBlocking()
                .exchange(HttpRequest.POST("/personal/balance/webhook/test-secret", PING_PAYLOAD)
                        .contentType(MediaType.APPLICATION_JSON))
                .status();
        assertEquals(HttpStatus.OK, status);
    }
}
