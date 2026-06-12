package com.home.budgetbot.bank.event;

import io.micronaut.core.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@ToString
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class BalanceChangeEvent {
    private String accountId;
    @Nullable
    private Integer oldBalance;
    @Nullable
    private Integer newBalance;
}
