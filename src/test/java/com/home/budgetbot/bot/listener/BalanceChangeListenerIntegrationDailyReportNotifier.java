package com.home.budgetbot.bot.listener;

import com.home.budgetbot.bank.event.BalanceChangeEvent;
import com.home.budgetbot.bank.event.BalanceScheduler;
import com.home.budgetbot.bank.service.BankService;
import com.home.budgetbot.bot.repository.entity.config.BudgetConfigEntity;
import com.home.budgetbot.bot.service.model.MessageModel;
import com.home.budgetbot.common.repository.DateTimeRepository;
import com.home.budgetbot.bot.service.ConfigService;
import com.home.budgetbot.bot.service.MessageService;
import com.home.budgetbot.bot.service.model.BudgetConfigModel;
import com.home.budgetbot.bot.service.model.ConfigModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("integration")
@MockBeans({@MockBean(BalanceScheduler.class), @MockBean(MessageService.class),
        @MockBean(ConfigService.class), @MockBean(DateTimeRepository.class)})
class BalanceChangeListenerIntegrationDailyReportNotifier {

    public static final String ACCOUNT_ID = "sae1d1scc13fSS";
    @Autowired
    private ConfigService configService;

    @Autowired
    private BalanceChangeListener balanceChangeListener;

    @Autowired
    private BankService bankService;

    @Autowired
    private DateTimeRepository timeRepository;

    @Autowired
    private MessageService messageService;

    @Captor
    private ArgumentCaptor<MessageModel> messageCaptor;

    private BudgetConfigModel budgetConfig;

    private OffsetDateTime date;

    @BeforeEach
    void setUp() {
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
    void shouldGenerateCorrectDayReport() {
        int balance = 24200;
        when(timeRepository.getNow()).thenReturn(date.minusDays(1));
        bankService.saveToHistory(ACCOUNT_ID, balance, 0);

        for (int i = 0; i < 10; i++) {
            date = date.plusMinutes(1);
            when(timeRepository.getNow()).thenReturn(date);
            balance = balance - 100;
            bankService.saveToHistory(ACCOUNT_ID, balance, 0);
        }

        balanceChangeListener.onBalanceChange(new BalanceChangeEvent(ACCOUNT_ID, balance, balance - 100));

        verify(messageService).notifyAll(messageCaptor.capture());

        MessageModel value = messageCaptor.getValue();

        assertEquals("Баланс изменился: -100\n" +
                "Дневной бюджет: -36\n" +
                "Глобальное отклонение: -1000", value.getMessage());

        //Next dat

        date = date.plusDays(1);

        when(timeRepository.getNow()).thenReturn(date);
        bankService.saveToHistory(ACCOUNT_ID, balance, 0);

        for (int i = 0; i < 4; i++) {
            date = date.plusMinutes(1);
            when(timeRepository.getNow()).thenReturn(date);
            balance = balance - 100;
            bankService.saveToHistory(ACCOUNT_ID, balance, 0);
        }

        balanceChangeListener.onBalanceChange(new BalanceChangeEvent(ACCOUNT_ID, balance, balance - 100));

        verify(messageService, times(2)).notifyAll(messageCaptor.capture());

        value = messageCaptor.getValue();

        assertEquals("Баланс изменился: -100\n" +
                "Дневной бюджет: 459\n" +
                "Глобальное отклонение: -1100", value.getMessage());
    }
}