package com.home.budgetbot.bot.repository.entity.config;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;

@Data
@Entity
@Accessors(chain = true)
public class BudgetConfigEntity extends ConfigEntity {
    private int salaryDay;
    private int budgetLimit;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "account_list")
    private Collection<String> accountList = new ArrayList<>();

    public BudgetConfigEntity() {
        this.setType(ConfigType.BUDGET);
    }
}
