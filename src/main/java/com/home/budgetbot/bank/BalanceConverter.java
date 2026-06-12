package com.home.budgetbot.bank;

import lombok.experimental.UtilityClass;

import java.math.BigInteger;

@UtilityClass
public class BalanceConverter {

    public static final BigInteger KOPIYKAS_IN_HRYVNA = BigInteger.valueOf(100);

    public static BigInteger balanceToUAH(BigInteger balance) {
        return balance.divide(KOPIYKAS_IN_HRYVNA);
    }

}
