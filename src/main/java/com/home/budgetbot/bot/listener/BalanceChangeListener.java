package com.home.budgetbot.bot.listener;

import com.home.budgetbot.bank.event.BalanceChangeEvent;
import com.home.budgetbot.bot.service.BudgetService;
import com.home.budgetbot.bot.service.ConfigService;
import com.home.budgetbot.bot.service.MessageService;
import com.home.budgetbot.bot.service.model.BudgetChangeReportModel;
import com.home.budgetbot.bot.service.model.ConfigModel;
import com.home.budgetbot.bot.service.model.MessageModel;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Singleton
public class BalanceChangeListener {

    public static final String MESSAGE_TEMPLATE = "Баланс изменился: %s\n"
            + "Дневной бюджет: %s\n"
            + "Глобальное отклонение: %s";

    @Inject
    ConfigService configService;

    @Inject
    MessageService messageService;

    @Inject
    BudgetService budgetService;

    @EventListener
    public void onBalanceChange(BalanceChangeEvent event) {
        ConfigModel config = configService.getConfig();
        String accountId = event.getAccountId();

        if (!config.getBudget().getAccountList().contains(accountId)) {
            return;
        }

        String balanceChange = Optional.ofNullable(event.getOldBalance())
                .map(oldValue -> oldValue - event.getNewBalance())
                .map(this::buildChangeString)
                .orElse("0");

        BudgetChangeReportModel report = budgetService.getBudgetChangeReport(event.getAccountId());

        String message = String.format(MESSAGE_TEMPLATE,
                balanceChange,
                mapDayBudgetState(report.getDayBudgetState()),
                addSign(report.getGlobalDeviation()));

        MessageModel model = new MessageModel()
                .setMessage(message);

        messageService.notifyAll(model);
    }

    private String mapDayBudgetState(Integer dayBudgetState) {
        if(dayBudgetState < 0) {
            return dayBudgetState + " 🤨";
        } else  {
            return String.valueOf(dayBudgetState);
        }
    }

    private String buildChangeString(Integer integer) {
        if (integer > 0) {
            return "-" + integer;
        } else {
            return "+" + Math.abs(integer);
        }
    }

    private String addSign(Integer integer) {
        if (integer < 0) {
            return String.valueOf(integer);
        } else {
            return "+" + integer;
        }
    }
}
