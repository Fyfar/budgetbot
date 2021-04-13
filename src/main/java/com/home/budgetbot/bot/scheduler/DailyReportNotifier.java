package com.home.budgetbot.bot.scheduler;

import com.home.budgetbot.bot.service.BudgetService;
import com.home.budgetbot.bot.service.ConfigService;
import com.home.budgetbot.bot.service.MessageService;
import com.home.budgetbot.bot.service.model.BudgetConfigModel;
import com.home.budgetbot.bot.service.model.DailyBudgetReportModel;
import com.home.budgetbot.bot.service.model.MessageModel;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class DailyReportNotifier {

    public static final String MESSAGE_TEMPLATE = "Account: %s\n"
            + "Бюджет на сегодня: %s\n"
            + "Предыдущий день: %s\n"
            + "Глобальное отклонение: %s";

    @Autowired
    private MessageService messageService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private ConfigService configService;

    @Scheduled(cron = "0 1 0 * * *")
    public void sendDailyReport() {
        BudgetConfigModel budgetConfig = configService.getConfig().getBudget();

        for (String accountId : budgetConfig.getAccountList()) {
            DailyBudgetReportModel report = budgetService.getDailyBudgetReport(accountId);

            String accountSecretString = generateSecret(accountId);

            String message = String.format(MESSAGE_TEMPLATE, accountSecretString, report.getDayBudget(),
                    report.getPreviousDayState(), report.getGlobalDeviation());

            MessageModel model = new MessageModel()
                    .addImage(report.getChartPath())
                    .setMessage(message);

            messageService.notifyAll(model);
        }
    }

    private String generateSecret(String accountId) {
        return accountId.substring(0, 5) + "*".repeat(accountId.length() - 4);
    }
}
