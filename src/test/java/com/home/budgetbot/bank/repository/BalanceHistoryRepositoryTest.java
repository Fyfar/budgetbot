package com.home.budgetbot.bank.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(environments = {"integration", "disableTelegramBot"})
class BalanceHistoryRepositoryTest {

    public static final String ACCOUNT_ID = "TEST";

    @Inject
    TestBalanceHistoryRepository historyRepository;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
    }

    @Test
    void shouldReturnLastValue() {
        OffsetDateTime time = OffsetDateTime.now();

        for (int i = 0; i < 10; i++) {
            time = time.plusDays(1);
            BalanceHistoryEntity entity = new BalanceHistoryEntity(ACCOUNT_ID + "_WRONG", i, time);
            historyRepository.save(entity);
        }

        for (int i = 10; i < 20; i++) {
            time = time.plusDays(1);
            BalanceHistoryEntity entity = new BalanceHistoryEntity(ACCOUNT_ID, i, time);
            historyRepository.save(entity);
        }

        for (int i = 20; i < 30; i++) {
            time = time.plusDays(1);
            BalanceHistoryEntity entity = new BalanceHistoryEntity(ACCOUNT_ID + "_WRONG", i, time);
            historyRepository.save(entity);
        }

        BalanceHistoryEntity historyEntity = historyRepository.findTop1ByAccountIdOrderByTimeDesc(ACCOUNT_ID);

        assertEquals(19, historyEntity.getBalance());
    }

    @Test
    void shouldReturnBalanceBeforeDate() {
        OffsetDateTime time = OffsetDateTime.now();
        int balance = 20000;

        for (int j = 0; j < 4; j++) {
            time = time.plusDays(1).withHour(0).withMinute(0).withSecond(0);
            for (int i = 0; i < 2; i++) {
                time = time.plusHours(1);
                balance = balance - 100;
                BalanceHistoryEntity entity = new BalanceHistoryEntity(ACCOUNT_ID, balance, time);
                historyRepository.save(entity);
            }
        }

        BalanceHistoryEntity balanceHistory = historyRepository.findLastBalanceBeforeTime(ACCOUNT_ID, time.minusDays(2).withHour(0).withMinute(0).withSecond(0)).get();

        assertEquals(19800, balanceHistory.getBalance());
    }
}
