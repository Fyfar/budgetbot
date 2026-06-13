package com.home.budgetbot.bot.repository.entity.config;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.Relation;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@MappedEntity("security_config")
public class SecurityConfigEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "securityConfigId")
    private List<AuthorizedUserEntry> authorizedUserList = new ArrayList<>();
}
