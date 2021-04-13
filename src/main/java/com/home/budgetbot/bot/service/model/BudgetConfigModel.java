package com.home.budgetbot.bot.service.model;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString
@Accessors(chain = true)
public class BudgetConfigModel {
    private int salaryDay;
    private int chartWidth;
    private int chartHeight;
    private int chartDefaultMaxValue;
    private int chartDefaultMinValue;
    private int budgetLimit;
    private List<String> accountList = new ArrayList<>();
}
