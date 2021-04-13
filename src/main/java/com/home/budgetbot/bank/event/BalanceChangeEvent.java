package com.home.budgetbot.bank.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

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
