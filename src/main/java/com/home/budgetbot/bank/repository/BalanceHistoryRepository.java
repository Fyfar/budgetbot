package com.home.budgetbot.bank.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BalanceHistoryRepository extends JpaRepository<BalanceHistoryEntity, UUID> {
    BalanceHistoryEntity findTop1ByAccountIdOrderByTimeDesc(String accountId);

    @Query("SELECT balance FROM BalanceHistoryEntity balance WHERE balance.accountId=:accountId AND balance.time < :time ORDER BY balance.time DESC")
    List<BalanceHistoryEntity> findBalanceBeforeTime(@Param("accountId") String accountId, @Param("time") OffsetDateTime time, Pageable pageable);

    default Optional<BalanceHistoryEntity> findLastBalanceBeforeTime(String accountId, OffsetDateTime time) {
        return findBalanceBeforeTime(accountId, time, PageRequest.of(0, 1)).stream().findFirst();
    }

    List<BalanceHistoryEntity> findByAccountIdAndTimeBetween(String accountId, OffsetDateTime from, OffsetDateTime to);
}
