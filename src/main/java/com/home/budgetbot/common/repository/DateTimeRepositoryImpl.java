package com.home.budgetbot.common.repository;

import jakarta.inject.Singleton;

import java.time.OffsetDateTime;

@Singleton
public class DateTimeRepositoryImpl implements DateTimeRepository {
    @Override
    public OffsetDateTime getNow() {
        return OffsetDateTime.now();
    }
}
