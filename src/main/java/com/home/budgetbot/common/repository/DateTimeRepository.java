package com.home.budgetbot.common.repository;

import java.time.OffsetDateTime;

public interface DateTimeRepository {
    OffsetDateTime getNow();
}
