package com.home.budgetbot.bank.service;

import com.home.budgetbot.bank.repository.BalanceHistoryEntity;
import com.home.budgetbot.bank.repository.BalanceHistoryRepository;
import com.home.budgetbot.bank.service.mapper.BankHistoryMapper;
import com.home.budgetbot.common.repository.DateTimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MonobankService implements BankService {

    @Autowired
    private BalanceHistoryRepository repository;

    @Autowired
    private DateTimeRepository dateTimeRepository;

    @Autowired
    private BankHistoryMapper bankHistoryMapper;

    @Override
    public Optional<Integer> findLastBalance(String accountId) {
        BalanceHistoryEntity history = repository.findTop1ByAccountIdOrderByTimeDesc(accountId);

        if (history == null) {
            return Optional.empty();
        }

        return Optional.of(history.getBalance());
    }

    @Override
    public List<BalanceHistoryModel> findBalanceHistoryByDay(String accountId, OffsetDateTime dateTime) {
        OffsetDateTime dayStart = getDayStart(dateTime);
        OffsetDateTime dayEnd = getDayEnd(dateTime);

        return repository.findByAccountIdAndTimeBetween(accountId, dayStart, dayEnd).stream()
                .map(bankHistoryMapper::map)
                .collect(Collectors.toList());
    }

    private OffsetDateTime getDayStart(OffsetDateTime dateTime) {
        return dateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    private OffsetDateTime getDayEnd(OffsetDateTime dateTime) {
        return dateTime.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
    }

    @Override
    public Optional<Integer> findInitialBalanceByDay(String accountId, OffsetDateTime dateTime) {
        OffsetDateTime dayStart = getDayStart(dateTime);
        Optional<Integer> balance = repository.findLastBalanceBeforeTime(accountId, dayStart).map(BalanceHistoryEntity::getBalance);

        if(balance.isPresent()) {
            return balance;
        }

        return repository.findByAccountIdAndTimeBetween(accountId, getDayStart(dateTime), getDayEnd(dateTime))
                .stream()
                .sorted(Comparator.comparing(BalanceHistoryEntity::getTime))
                .map(BalanceHistoryEntity::getBalance)
                .findFirst();
    }

    @Override
    public Optional<Integer> findBalanceDifferenceByDay(String accountId, OffsetDateTime dateTime) {
        List<Integer> balanceHistory = findBalanceHistoryByDay(accountId, dateTime)
                .stream().map(BalanceHistoryModel::getBalance).collect(Collectors.toList());

        if(balanceHistory.isEmpty()) {
            return Optional.empty();
        }

        Integer firstDayBalance = repository.findLastBalanceBeforeTime(accountId, getDayStart(dateTime))
                .map(BalanceHistoryEntity::getBalance)
                .orElse(balanceHistory.get(0));

        if(balanceHistory.size() == 1) {
            return Optional.of(firstDayBalance - balanceHistory.get(0));
        }

        int first = firstDayBalance;
        int last = balanceHistory.get(balanceHistory.size() - 1);

        return Optional.of(first - last);
    }

    @Override
    public void saveToHistory(String accountId, int balance, int penny) {
        OffsetDateTime now = dateTimeRepository.getNow();
        BalanceHistoryEntity history = new BalanceHistoryEntity(accountId, balance, penny, now);
        repository.save(history);
    }
}
