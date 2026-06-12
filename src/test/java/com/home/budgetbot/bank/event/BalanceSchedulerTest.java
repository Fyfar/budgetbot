package com.home.budgetbot.bank.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.home.budgetbot.bank.BalanceChangeEventListener;
import com.home.budgetbot.bank.model.BalanceChangedEvent;
import com.home.budgetbot.bank.model.BalanceChangedWebhookInput;
import com.home.budgetbot.bank.model.BalanceChangedWebhookInput.AccountData;
import com.home.budgetbot.bank.repository.TestBalanceHistoryRepository;
import com.home.budgetbot.bank.service.BalanceService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

@MicronautTest(environments = {"integration", "disableTelegramBot"})
class BalanceSchedulerTest {
    public static final String ACCOUNT_ID = "q2esff254";

    @Inject
    BalanceService balanceService;

    @Inject
    TestBalanceHistoryRepository historyRepository;

    @Inject
    BalanceChangeEventListener eventListener;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        eventListener.clean();
    }

    @Test
    void shouldFireEventWithNullOldValue() {
        BalanceChangedEvent balance = new BalanceChangedEvent("tx-first-event", Instant.now(), "descr", BigInteger.ONE, BigInteger.valueOf(10022), false);
        BalanceChangedWebhookInput input = new BalanceChangedWebhookInput("type", new AccountData(ACCOUNT_ID, balance));
        balanceService.balanceChanged(input);

        List<BalanceChangeEvent> eventList = eventListener.getEventList();

        assertEquals(1, eventList.size());
        assertEquals(100, eventList.get(0).getNewBalance());
        assertNull(eventList.get(0).getOldBalance());
        assertNotNull(eventList.get(0).getAccountId());
    }

    @Test
    void shouldNotFireEventOnSettlementAfterHold() {
        // hold=true arrives first → fires notification
        BalanceChangedEvent holdPayload = new BalanceChangedEvent("tx-hold-settle", Instant.now(), "descr", BigInteger.ONE, BigInteger.valueOf(10022), true);
        BalanceChangedWebhookInput holdInput = new BalanceChangedWebhookInput("type", new AccountData(ACCOUNT_ID, holdPayload));
        balanceService.balanceChanged(holdInput);

        // hold=false (settlement) arrives with same transaction id → deduplicated
        BalanceChangedEvent settlePayload = new BalanceChangedEvent("tx-hold-settle", Instant.now(), "descr", BigInteger.ONE, BigInteger.valueOf(10022), false);
        BalanceChangedWebhookInput settleInput = new BalanceChangedWebhookInput("type", new AccountData(ACCOUNT_ID, settlePayload));
        balanceService.balanceChanged(settleInput);

        assertEquals(1, eventListener.getEventList().size());
    }

    @Test
    void shouldNotFireEventWhenBalanceUnchanged() {
        BalanceChangedEvent firstPayload = new BalanceChangedEvent("tx-dedup", Instant.now(), "descr", BigInteger.ONE, BigInteger.valueOf(10022), false);
        BalanceChangedWebhookInput firstInput = new BalanceChangedWebhookInput("type", new AccountData(ACCOUNT_ID, firstPayload));
        balanceService.balanceChanged(firstInput);

        // Same balance again — deduplication guard should suppress the second event
        balanceService.balanceChanged(firstInput);

        List<BalanceChangeEvent> eventList = eventListener.getEventList();

        assertEquals(1, eventList.size());
        assertEquals(100, eventList.get(0).getNewBalance());
    }
}
