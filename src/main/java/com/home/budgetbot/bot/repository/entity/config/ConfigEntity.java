package com.home.budgetbot.bot.repository.entity.config;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Data
@Entity
@Accessors(chain = true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class ConfigEntity {
    @Id
    @Enumerated(EnumType.STRING)
    protected ConfigType type;
}
