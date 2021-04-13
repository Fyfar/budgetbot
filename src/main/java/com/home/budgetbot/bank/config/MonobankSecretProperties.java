package com.home.budgetbot.bank.config;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class MonobankSecretProperties {
    private List<String> tokenList = new ArrayList<>();
}
