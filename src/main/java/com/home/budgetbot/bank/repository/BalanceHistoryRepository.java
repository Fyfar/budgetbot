package com.home.budgetbot.bank.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.GenericRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BalanceHistoryRepository extends GenericRepository<BalanceHistoryEntity, String> {

    BalanceHistoryEntity save(BalanceHistoryEntity entity);

    void deleteAll();

    @Query("SELECT b FROM BalanceHistoryEntity b WHERE b.accountId = :accountId ORDER BY b.time DESC")
    List<BalanceHistoryEntity> findByAccountIdOrderByTimeDescPaged(String accountId, Pageable pageable);

    default BalanceHistoryEntity findTop1ByAccountIdOrderByTimeDesc(String accountId) {
        return findByAccountIdOrderByTimeDescPaged(accountId, Pageable.from(0, 1))
                .stream().findFirst().orElse(null);
    }

    @Query("SELECT b FROM BalanceHistoryEntity b WHERE b.accountId = :accountId AND b.time < :time ORDER BY b.time DESC")
    List<BalanceHistoryEntity> findBalanceBeforeTime(String accountId, OffsetDateTime time, Pageable pageable);

    default Optional<BalanceHistoryEntity> findLastBalanceBeforeTime(String accountId, OffsetDateTime time) {
        return findBalanceBeforeTime(accountId, time, Pageable.from(0, 1)).stream().findFirst();
    }

    @Query("SELECT b FROM BalanceHistoryEntity b WHERE b.accountId = :accountId AND b.time BETWEEN :from AND :to")
    List<BalanceHistoryEntity> findByAccountIdAndTimeBetween(String accountId, OffsetDateTime from, OffsetDateTime to);
}
