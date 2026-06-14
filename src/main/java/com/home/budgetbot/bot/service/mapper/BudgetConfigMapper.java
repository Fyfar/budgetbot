package com.home.budgetbot.bot.service.mapper;

import com.home.budgetbot.bot.repository.entity.config.AccountListEntry;
import com.home.budgetbot.bot.repository.entity.config.BudgetConfigEntity;
import com.home.budgetbot.bot.service.model.BudgetConfigModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jsr330")
public interface BudgetConfigMapper {

    @Mapping(target = "accountList", source = "accountList")
    BudgetConfigModel map(BudgetConfigEntity entity);

    default String toAccount(AccountListEntry entry) {
        return entry == null ? null : entry.getAccount();
    }
}
