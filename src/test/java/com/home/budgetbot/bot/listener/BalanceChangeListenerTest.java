package com.home.budgetbot.bot.listener;

import com.home.budgetbot.bank.event.BalanceChangeEvent;
import com.home.budgetbot.bank.service.BankService;
import com.home.budgetbot.bot.service.ConfigService;
import com.home.budgetbot.bot.service.MessageService;
import com.home.budgetbot.bot.service.model.BudgetConfigModel;
import com.home.budgetbot.bot.service.model.ConfigModel;
import com.home.budgetbot.bot.service.model.MessageModel;
import com.home.budgetbot.common.repository.DateTimeRepository;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MicronautTest(environments = {"integration", "disableTelegramBot"})
class BalanceChangeListenerTest {

    public static final String ACCOUNT_ID = "sae1d1scc13fSS";

    @Inject
    ConfigService configService;

    @Inject
    BalanceChangeListener balanceChangeListener;

    @Inject
    BankService bankService;

    @Inject
    DateTimeRepository timeRepository;

    @Inject
    MessageService messageService;

    @MockBean(ConfigService.class)
    ConfigService configServiceMock() {
        return mock(ConfigService.class);
    }

    @MockBean(DateTimeRepository.class)
    DateTimeRepository timeRepositoryMock() {
        return mock(DateTimeRepository.class);
    }

    @MockBean(MessageService.class)
    MessageService messageServiceMock() {
        return mock(MessageService.class);
    }

    private final ArgumentCaptor<MessageModel> messageCaptor = ArgumentCaptor.forClass(MessageModel.class);

    private BudgetConfigModel budgetConfig;

    private OffsetDateTime date;

    @BeforeEach
    void setUp() {
        budgetConfig = new BudgetConfigModel()
                .setSalaryDay(5)
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
                "Дневной бюджет: -136 🤨\n" +
                "Глобальное отклонение: -1000", value.getMessage());

        //Next day

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
