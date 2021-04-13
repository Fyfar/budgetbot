package com.home.budgetbot.bot.repository.entity.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

@Data
@Entity
@Accessors(chain = true)
public class SecurityConfigEntity extends ConfigEntity {

    @ElementCollection
    @CollectionTable(name = "authorized_user_list")
    @LazyCollection(LazyCollectionOption.FALSE)
    private Collection<Integer> authorizedUserList = new ArrayList<>();

    public SecurityConfigEntity() {
        this.setType(ConfigType.SECURITY);
    }
}
