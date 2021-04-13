package com.home.budgetbot.bot.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class NumberStatisticModel {
    private Integer value;
    private OffsetDateTime date;
}
