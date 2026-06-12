package com.home.budgetbot.bank.service;

import com.home.budgetbot.bank.repository.BalanceHistoryEntity;
import com.home.budgetbot.bank.repository.TestBalanceHistoryRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(environments = {"integration", "disableTelegramBot"})
class MonobankServiceTest {
    public static final String ACCOUNT_ID = "TEST";

    @Inject
    TestBalanceHistoryRepository historyRepository;

    @Inject
    BankService bankService;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
    }

    @Test
    void shouldReturnEmptyWhenNoHistory() {
        Optional<Integer> response = bankService.findBalanceDifferenceByDay(ACCOUNT_ID, OffsetDateTime.now());

        assertTrue(response.isEmpty());
    }

    @Test
    void shouldReturnZeroWhenOneHistoryEntity() {
        OffsetDateTime now = OffsetDateTime.now();

        historyRepository.save(new BalanceHistoryEntity(ACCOUNT_ID, 100, now));

        Optional<Integer> response = bankService.findBalanceDifferenceByDay(ACCOUNT_ID, OffsetDateTime.now());
        assertFalse(response.isEmpty());
        assertEquals(0, response.get());
    }

    @Test
    void shouldReturnDifferenceWhenHaveHistory() {
        OffsetDateTime now = OffsetDateTime.now();

        historyRepository.save(new BalanceHistoryEntity(ACCOUNT_ID, 100, now));
        now = now.plusMinutes(1);
        historyRepository.save(new BalanceHistoryEntity(ACCOUNT_ID, 50, now));
        now = now.plusMinutes(1);
        historyRepository.save(new BalanceHistoryEntity(ACCOUNT_ID, 25, now));

        now = now.plusDays(1);
        historyRepository.save(new BalanceHistoryEntity(ACCOUNT_ID, 25, now));
        now = now.minusDays(1);
        historyRepository.save(new BalanceHistoryEntity(ACCOUNT_ID, 25, now));

        Optional<Integer> response = bankService.findBalanceDifferenceByDay(ACCOUNT_ID, OffsetDateTime.now());
        assertFalse(response.isEmpty());
        assertEquals(75, response.get());
    }
}
