package com.home.budgetbot.bot.repository.entity.config;


import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

@Data
@Entity
@Accessors(chain = true)
public class BudgetConfigEntity extends ConfigEntity {
    private int salaryDay;
    private int chartWidth;
    private int chartHeight;
    private int chartDefaultMaxValue;
    private int chartDefaultMinValue;
    private int budgetLimit;

    @ElementCollection
    @CollectionTable(name = "account_list")
    @LazyCollection(LazyCollectionOption.FALSE)
    private Collection<String> accountList = new ArrayList<>();

    public BudgetConfigEntity() {
        this.setType(ConfigType.BUDGET);
    }
}
