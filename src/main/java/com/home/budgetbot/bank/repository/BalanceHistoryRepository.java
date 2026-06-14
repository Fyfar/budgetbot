package com.home.budgetbot.bank.repository;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * All finders are Micronaut Data derived queries: the SQL is generated and
 * validated at compile time from the method name — no hand-written @Query, no
 * Pageable plumbing. CrudRepository supplies save/deleteAll/count/findById.
 */
@JdbcRepository(dialect = Dialect.H2)
public interface BalanceHistoryRepository extends CrudRepository<BalanceHistoryEntity, String> {

    Optional<BalanceHistoryEntity> findFirstByAccountIdOrderByTimeDesc(String accountId);

    Optional<BalanceHistoryEntity> findFirstByAccountIdAndTimeLessThanOrderByTimeDesc(String accountId, OffsetDateTime time);

    Optional<BalanceHistoryEntity> findFirstByAccountIdAndTimeBetweenOrderByTimeAsc(String accountId, OffsetDateTime from, OffsetDateTime to);

    List<BalanceHistoryEntity> findByAccountIdAndTimeBetween(String accountId, OffsetDateTime from, OffsetDateTime to);
}
