package com.home.budgetbot.common.repository;

import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public class DateTimeRepositoryImpl implements DateTimeRepository {
    @Override
    public OffsetDateTime getNow() {
        return OffsetDateTime.now();
    }
}
