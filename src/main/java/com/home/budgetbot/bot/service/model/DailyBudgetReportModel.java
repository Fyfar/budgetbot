package com.home.budgetbot.bot.service.model;

import lombok.Data;

@Data
public class DailyBudgetReportModel {
    private Integer dayBudget;
    private Integer globalDeviation;
    private Integer previousDayState;
}
