package com.home.budgetbot.bot.service.mapper;

import com.home.budgetbot.bot.repository.entity.config.BudgetConfigEntity;
import com.home.budgetbot.bot.service.model.BudgetConfigModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BudgetConfigMapper {
    BudgetConfigModel map(BudgetConfigEntity entity);
    BudgetConfigEntity map(BudgetConfigModel model);
}
