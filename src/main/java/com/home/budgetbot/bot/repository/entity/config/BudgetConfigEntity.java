package com.home.budgetbot.bot.repository.entity.config;


import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.Collection;

@Data
@Entity
@Accessors(chain = true)
public class BudgetConfigEntity extends ConfigEntity {
    private int salaryDay;
    private int budgetLimit;

    @ElementCollection
    @CollectionTable(name = "account_list")
    @LazyCollection(LazyCollectionOption.FALSE)
    private Collection<String> accountList = new ArrayList<>();

    public BudgetConfigEntity() {
        this.setType(ConfigType.BUDGET);
    }
}
