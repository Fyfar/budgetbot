package com.home.budgetbot.bank.service;

import com.home.budgetbot.bank.event.BalanceScheduler;
import com.home.budgetbot.bank.repository.BalanceHistoryEntity;
import com.home.budgetbot.bank.repository.BalanceHistoryRepository;
import com.home.budgetbot.bot.listener.TelegramBotUpdateListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("integration")
@MockBeans({@MockBean(BalanceScheduler.class), @MockBean(TelegramBotUpdateListener.class)})
class MonobankServiceDailyReportNotifier {
    public static final String ACCOUNT_ID = "TEST";

    @Autowired
    private BalanceHistoryRepository historyRepository;

    @Autowired
    private BankService bankService;

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