package com.home.budgetbot.bot.service.model;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@ToString
public class ConfigModel {
    private BudgetConfigModel budget;
    private SecurityConfigModel security;
}
