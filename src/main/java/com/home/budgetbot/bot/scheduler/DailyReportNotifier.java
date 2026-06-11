package com.home.budgetbot.bot.scheduler;

import com.home.budgetbot.bot.service.BudgetService;
import com.home.budgetbot.bot.service.ConfigService;
import com.home.budgetbot.bot.service.MessageService;
import com.home.budgetbot.bot.service.model.BudgetConfigModel;
import com.home.budgetbot.bot.service.model.DailyBudgetReportModel;
import com.home.budgetbot.bot.service.model.MessageModel;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DailyReportNotifier {

    public static final String MESSAGE_TEMPLATE = "Account: %s\n"
            + "Бюджет на сегодня: %s\n"
            + "Предыдущий день: %s\n"
            + "Глобальное отклонение: %s";

    @Inject
    MessageService messageService;

    @Inject
    BudgetService budgetService;

    @Inject
    ConfigService configService;

    @Scheduled(cron = "0 1 0 * * *")
    public void sendDailyReport() {
        BudgetConfigModel budgetConfig = configService.getConfig().getBudget();

        for (String accountId : budgetConfig.getAccountList()) {
            DailyBudgetReportModel report = budgetService.getDailyBudgetReport(accountId);

            String accountSecretString = generateSecret(accountId);

            String message = String.format(MESSAGE_TEMPLATE,
                    accountSecretString,
                    mapDayBudget(report.getDayBudget()),
                    mapPreviousDayState(report.getPreviousDayState()),
                    mapGlobalDeviation(report.getGlobalDeviation()));

            MessageModel model = new MessageModel()
                    .setMessage(message);

            messageService.notifyAll(model);
        }
    }

    private String mapGlobalDeviation(Integer globalDeviation) {
        if(globalDeviation == null) {
            return "🤷";
        }

        String smile = "🙂";

        if(globalDeviation < -3000) {
            smile = "🤬";
        } else if(globalDeviation < -1000) {
            smile = "😡";
        } else if(globalDeviation < 0) {
            smile = "😤";
        } else if(globalDeviation == 0) {
            smile = "😐";
        } else if(globalDeviation > 5000) {
            smile = "😈";
        } else if(globalDeviation > 2000) {
            smile = "🤑";
        } else if(globalDeviation > 1500) {
            smile = "😀";
        } else if(globalDeviation > 1000) {
            smile = "😲";
        } else if(globalDeviation > 500) {
            smile = "🙃";
        }

        return addSign(globalDeviation) + " " + smile;
    }

    private String mapPreviousDayState(Integer previousDayState) {
        if(previousDayState == null) {
            return "🤷";
        }

        if(previousDayState >= 0) {
            return addSign(previousDayState) + " 👍";
        } else {
            return addSign(previousDayState) + " 👎";
        }
    }

    private String mapDayBudget(Integer dayBudget) {
        if(dayBudget == null) {
            return "🤷";
        }

        int budgetLimit = configService.getConfig().getBudget().getBudgetLimit();

        if (budgetLimit >= dayBudget) {
            return dayBudget + " 👌";
        } else {
            return dayBudget + " 🤞";
        }
    }

    private String generateSecret(String accountId) {
        return accountId.substring(0, 5) + "*".repeat(accountId.length() - 4);
    }

    private String addSign(Integer integer) {
        if (integer < 0) {
            return String.valueOf(integer);
        } else {
            return "+" + integer;
        }
    }
}
