package com.home.budgetbot.bank.client;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@ToString
@Accessors(chain = true)
public class ClientInfoDto {
    private String id;
    private String name;
    private String webHookUrl;
    private List<AccountDto> accounts;
}
