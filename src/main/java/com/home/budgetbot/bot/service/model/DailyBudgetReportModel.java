package com.home.budgetbot.bot.service.model;

import lombok.Data;

@Data
public class DailyBudgetReportModel {
    private String dayBudget;
    private String globalDeviation;
    private String previousDayState;
}
