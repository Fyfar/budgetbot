package com.home.budgetbot.bot.repository.entity.config;

import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Entity
@Accessors(chain = true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class ConfigEntity {
    @Id
    @Enumerated(EnumType.STRING)
    protected ConfigType type;
}
