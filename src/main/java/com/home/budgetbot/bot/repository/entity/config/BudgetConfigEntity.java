package com.home.budgetbot.bot.repository.entity.config;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.Relation;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@MappedEntity("budget_config")
public class BudgetConfigEntity {
    @Id
    @GeneratedValue
    private Long id;

    private int salaryDay;
    private int budgetLimit;

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "budgetConfigId")
    private List<AccountListEntry> accountList = new ArrayList<>();
}
