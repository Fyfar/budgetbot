package com.home.budgetbot.bank.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.home.budgetbot.bank.BalanceChangeEventListener;
import com.home.budgetbot.bank.model.BalanceChangedEvent;
import com.home.budgetbot.bank.model.BalanceChangedWebhookInput;
import com.home.budgetbot.bank.model.BalanceChangedWebhookInput.AccountData;
import com.home.budgetbot.bank.repository.BalanceHistoryRepository;
import com.home.budgetbot.bank.service.BalanceService;
import com.home.budgetbot.bot.listener.TelegramBotUpdateListener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("integration")
@MockBeans({@MockBean(TelegramBotUpdateListener.class)})
class BalanceSchedulerTest {
    public static final String ACCOUNT_ID = "q2esff254";

    @Autowired
    private BalanceService balanceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private BalanceHistoryRepository historyRepository;

    @Autowired
    private BalanceChangeEventListener eventListener;

    @BeforeAll
    static void beforeAll() {
        WireMockServer wireMockServer = new WireMockServer(8080);
        wireMockServer.start();
    }

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        eventListener.clean();
    }

    @Test
    void shouldFireEventWithNullOldValue() {
        BalanceChangedEvent balance = new BalanceChangedEvent(Instant.now(), "descr", BigInteger.ONE, BigInteger.valueOf(10022));
        BalanceChangedWebhookInput input = new BalanceChangedWebhookInput("type", new AccountData(ACCOUNT_ID), balance);
        balanceService.balanceChanged(input);

        List<BalanceChangeEvent> eventList = eventListener.getEventList();

        assertEquals(1, eventList.size());
        assertEquals(100, eventList.get(0).getNewBalance());
        assertNull(eventList.get(0).getOldBalance());
        assertNotNull(eventList.get(0).getAccountId());
    }

    @Test
    void shouldNotFireEventWhenNoChanges() {
        BalanceChangedEvent balance = new BalanceChangedEvent(Instant.now(), "descr", BigInteger.ZERO, BigInteger.valueOf(10023));
        BalanceChangedWebhookInput input = new BalanceChangedWebhookInput("type", new AccountData(ACCOUNT_ID), balance);
        balanceService.balanceChanged(input);

        List<BalanceChangeEvent> eventList = eventListener.getEventList();

        assertEquals(1, eventList.size());
        assertEquals(100, eventList.get(0).getNewBalance());
        assertNull(eventList.get(0).getOldBalance());
        assertNotNull(eventList.get(0).getAccountId());
    }
}