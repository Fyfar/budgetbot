package com.home.budgetbot.bot.service;

import com.home.budgetbot.bank.event.BalanceScheduler;
import com.home.budgetbot.bank.repository.BalanceHistoryRepository;
import com.home.budgetbot.bank.service.BankService;
import com.home.budgetbot.bot.listener.TelegramBotUpdateListener;
import com.home.budgetbot.bot.service.model.BudgetChangeReportModel;
import com.home.budgetbot.bot.service.model.BudgetConfigModel;
import com.home.budgetbot.bot.service.model.ConfigModel;
import com.home.budgetbot.bot.service.model.DailyBudgetReportModel;
import com.home.budgetbot.common.repository.DateTimeRepository;
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
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles({"integration", "disableTelegramBot"})
@MockBeans({@MockBean(BalanceScheduler.class), @MockBean(MessageService.class),
        @MockBean(ConfigService.class), @MockBean(DateTimeRepository.class), @MockBean(TelegramBotUpdateListener.class)})
class BudgetServiceTest {

    public static final String ACCOUNT_ID = "sae1d1scc13fSS";

    @Autowired
    private ConfigService configService;

    @Autowired
    private DateTimeRepository timeRepository;

    @Autowired
    private BankService bankService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private BalanceHistoryRepository historyRepository;

    private OffsetDateTime date;

    private BudgetConfigModel budgetConfig;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();

        budgetConfig = new BudgetConfigModel()
                .setSalaryDay(5)
                .setChartWidth(800)
                .setChartHeight(600)
                .setChartDefaultMaxValue(2000)
                .setChartDefaultMinValue(200)
                .setAccountList(Arrays.asList(ACCOUNT_ID))
                .setBudgetLimit(900);

        when(configService.getConfig()).thenReturn(new ConfigModel().setBudget(budgetConfig));

        date = OffsetDateTime.now()
                .withYear(2021)
                .withMonth(6)
                .withDayOfMonth(8)
                .withHour(10);
    }

    @Test
    void secondChangeByDay() {
        when(timeRepository.getNow()).thenReturn(date.minusDays(1));
        bankService.saveToHistory(ACCOUNT_ID, 25000, 0);

        when(timeRepository.getNow()).thenReturn(date);
        bankService.saveToHistory(ACCOUNT_ID, 24900, 0);

        date = date.plusMinutes(1);
        when(timeRepository.getNow()).thenReturn(date);
        bankService.saveToHistory(ACCOUNT_ID, 24800, 0);

        date = date.plusMinutes(1);
        when(timeRepository.getNow()).thenReturn(date);
        BudgetChangeReportModel report = budgetService.getBudgetChangeReport(ACCOUNT_ID);

        assertEquals("692", report.getDayBudgetState());
    }

    @Test
    void negativeCase() {
        int balance = 24200;

        for (int i = 0; i < 10; i++) {
            date = date.plusMinutes(1);
            when(timeRepository.getNow()).thenReturn(date);
            balance = balance - 100;
            bankService.saveToHistory(ACCOUNT_ID, balance, 0);
        }

        date = date.plusDays(1);

        for (int i = 0; i < 10; i++) {
            date = date.plusMinutes(1);
            when(timeRepository.getNow()).thenReturn(date);
            balance = balance - 100;
            bankService.saveToHistory(ACCOUNT_ID, balance, 0);
        }

        DailyBudgetReportModel report = budgetService.getDailyBudgetReport(ACCOUNT_ID);

        assertEquals("859", report.getDayBudget());
        assertEquals("-1100", report.getGlobalDeviation());
        assertEquals("-40", report.getPreviousDayState());
    }

    @Test
    void positiveCase() {
        int balance = 34200;

        for (int i = 0; i < 10; i++) {
            date = date.plusMinutes(1);
            when(timeRepository.getNow()).thenReturn(date);
            balance = balance - 100;
            bankService.saveToHistory(ACCOUNT_ID, balance, 0);
        }

        date = date.plusDays(1);

        for (int i = 0; i < 10; i++) {
            date = date.plusMinutes(1);
            when(timeRepository.getNow()).thenReturn(date);
            balance = balance - 100;
            bankService.saveToHistory(ACCOUNT_ID, balance, 0);
        }

        DailyBudgetReportModel report = budgetService.getDailyBudgetReport(ACCOUNT_ID);

        assertEquals("1229", report.getDayBudget());
        assertEquals("+8900", report.getGlobalDeviation());
        assertEquals("+317", report.getPreviousDayState());
    }

    @Test
    void shouldUsePreviousDayStatisticToFindDayDifference() {
        int balance = 15537 + 21;

        when(timeRepository.getNow()).thenReturn(date.minusDays(1));
        bankService.saveToHistory(ACCOUNT_ID, balance, 0);

        when(timeRepository.getNow()).thenReturn(date);
        DailyBudgetReportModel dailyBudgetReport = budgetService.getDailyBudgetReport(ACCOUNT_ID);

        assertEquals("555", dailyBudgetReport.getDayBudget());

        when(timeRepository.getNow()).thenReturn(date);
        bankService.saveToHistory(ACCOUNT_ID, balance - 21, 0);

        BudgetChangeReportModel budgetChangeReport = budgetService.getBudgetChangeReport(ACCOUNT_ID);

        assertEquals("534", budgetChangeReport.getDayBudgetState());
    }
}