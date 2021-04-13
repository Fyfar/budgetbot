package com.home.budgetbot.bot.listener;

import com.home.budgetbot.bank.event.BalanceChangeEvent;
import com.home.budgetbot.bot.service.BudgetService;
import com.home.budgetbot.bot.service.ConfigService;
import com.home.budgetbot.bot.service.MessageService;
import com.home.budgetbot.bot.service.model.BudgetChangeReportModel;
import com.home.budgetbot.bot.service.model.ConfigModel;
import com.home.budgetbot.bot.service.model.MessageModel;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Log4j2
@Component
public class BalanceChangeListener {

    public static final String MESSAGE_TEMPLATE = "Баланс изменился: %s\n"
            + "Дневной бюджет: %s\n"
            + "Глобальное отклонение: %s";

    @Autowired
    private ConfigService configService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private BudgetService budgetService;

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

        String message = String.format(MESSAGE_TEMPLATE, balanceChange, report.getDayBudgetState(), report.getGlobalDeviation());
        MessageModel model = new MessageModel()
                .addImage(report.getChartPath())
                .setMessage(message);

        messageService.notifyAll(model);
    }

    private String buildChangeString(Integer integer) {
        if(integer > 0) {
            return "-"+integer;
        } else {
            return "+"+Math.abs(integer);
        }
    }
}
