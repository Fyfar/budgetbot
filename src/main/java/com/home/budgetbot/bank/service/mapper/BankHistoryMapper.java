package com.home.budgetbot.bank.service.mapper;

import com.home.budgetbot.bank.repository.BalanceHistoryEntity;
import com.home.budgetbot.bank.service.BalanceHistoryModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BankHistoryMapper {
    BalanceHistoryModel map(BalanceHistoryEntity entity);

    BalanceHistoryEntity map(BalanceHistoryModel model);
}
