package com.home.budgetbot.bot.service;

import com.home.budgetbot.bank.service.BankService;
import com.home.budgetbot.bot.service.model.BudgetChangeReportModel;
import com.home.budgetbot.bot.service.model.ConfigModel;
import com.home.budgetbot.bot.service.model.DailyBudgetReportModel;
import com.home.budgetbot.common.repository.DateTimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.BiFunction;

@Service
public class BudgetService {

    @Autowired
    private BankService bankService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private DateTimeRepository dateTimeRepository;

    public BudgetChangeReportModel getBudgetChangeReport(String accountId) {
        OffsetDateTime now = dateTimeRepository.getNow();

        Optional<Integer> dayBudgetOptional = getDayBudget(accountId, now);
        Optional<Integer> balanceDifferenceOptional = bankService.findBalanceDifferenceByDay(accountId, now);

        BudgetChangeReportModel report = new BudgetChangeReportModel();

        both(dayBudgetOptional, balanceDifferenceOptional, (dayBudget, balanceDifference) -> dayBudget - balanceDifference)
                .map(String::valueOf)
                .ifPresent(report::setDayBudgetState);

        getGlobalDeviation(accountId, now)
                .map(this::addSign)
                .ifPresent(report::setGlobalDeviation);

        return report;
    }

    public DailyBudgetReportModel getDailyBudgetReport(String accountId) {
        OffsetDateTime now = dateTimeRepository.getNow();

        DailyBudgetReportModel model = new DailyBudgetReportModel();

        getDayBudget(accountId, now)
                .map(String::valueOf)
                .ifPresent(model::setDayBudget);

        getGlobalDeviation(accountId, now)
                .map(this::addSign)
                .ifPresent(model::setGlobalDeviation);

        Optional<Integer> previousDayBudget = getDayBudget(accountId, now.minusDays(1));
        Optional<Integer> balanceDifference = bankService.findBalanceDifferenceByDay(accountId, now.minusDays(1));

        both(previousDayBudget, balanceDifference, (left, right) -> left - right)
                .map(this::addSign)
                .ifPresent(model::setPreviousDayState);

        return model;
    }

    private String addSign(Integer integer) {
        if (integer < 0) {
            return String.valueOf(integer);
        } else {
            return "+" + integer;
        }
    }

    private Optional<Integer> getGlobalDeviation(String accountId, OffsetDateTime day) {
        ConfigModel config = configService.getConfig();

        int salaryDay = config.getBudget().getSalaryDay();
        int daysCountTillSalary = getDaysCountTillSalary(day, salaryDay);

        int budgetLimit = config.getBudget().getBudgetLimit();

        int expectedBalance = (daysCountTillSalary * budgetLimit);

        Optional<Integer> lastBalance = bankService.findInitialBalanceByDay(accountId, day);

        return lastBalance.map(balance -> balance - expectedBalance);
    }

    private Optional<Integer> getDayBudget(String accountId, OffsetDateTime day) {
        ConfigModel config = configService.getConfig();

        Optional<Integer> initialBalanceByDay = bankService.findInitialBalanceByDay(accountId, day);
        int daysCountTillSalary = getDaysCountTillSalary(day, config.getBudget().getSalaryDay());

        return initialBalanceByDay
                .map(initialBalance -> initialBalance / daysCountTillSalary)
                .map(dailyLimit -> {
                    int budgetLimit = config.getBudget().getBudgetLimit();

                    if (dailyLimit > budgetLimit) {
                        return budgetLimit;
                    } else {
                        return dailyLimit;
                    }
                });
    }

    private <F, S, T> Optional<T> both(Optional<F> left, Optional<S> right, BiFunction<F, S, T> mergeStrategy) {
        if (left.isEmpty() || right.isEmpty()) {
            return Optional.empty();
        }

        F leftValue = left.get();
        S rightValue = right.get();

        T result = mergeStrategy.apply(leftValue, rightValue);
        return Optional.ofNullable(result);
    }

    protected int getDaysCountTillSalary(OffsetDateTime day, int salaryDay) {
        int count = 1;

        for (OffsetDateTime date = day; date.getDayOfMonth() != salaryDay; date = date.plusDays(1)) {
            count++;
        }

        return count;
    }
}
