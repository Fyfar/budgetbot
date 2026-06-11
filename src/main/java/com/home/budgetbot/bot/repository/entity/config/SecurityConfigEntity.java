package com.home.budgetbot.bot.repository.entity.config;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;

@Data
@Entity
@Accessors(chain = true)
public class SecurityConfigEntity extends ConfigEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "authorized_user_list")
    private Collection<Integer> authorizedUserList = new ArrayList<>();

    public SecurityConfigEntity() {
        this.setType(ConfigType.SECURITY);
    }
}
