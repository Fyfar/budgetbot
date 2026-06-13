package com.home.budgetbot.bot.service.mapper;

import com.home.budgetbot.bot.repository.entity.config.AuthorizedUserEntry;
import com.home.budgetbot.bot.repository.entity.config.SecurityConfigEntity;
import com.home.budgetbot.bot.service.model.SecurityConfigModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jsr330")
public interface SecurityConfigMapper {

    @Mapping(target = "authorizedUserList", source = "authorizedUserList")
    SecurityConfigModel map(SecurityConfigEntity entity);

    default Integer toUserId(AuthorizedUserEntry entry) {
        return entry == null ? null : entry.getUserId();
    }
}
