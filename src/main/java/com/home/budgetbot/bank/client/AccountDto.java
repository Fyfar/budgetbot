package com.home.budgetbot.bank.client;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@ToString
@Accessors(chain = true)
public class AccountDto {
    private String id;
    private int balance;
    private int creditLimit;
    private String type;
    private int currencyCode;
    private String cashbackType;
}
