package com.home.budgetbot.bot.service.mapper;

import com.home.budgetbot.bot.repository.entity.config.SecurityConfigEntity;
import com.home.budgetbot.bot.service.model.SecurityConfigModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SecurityConfigMapper {
    SecurityConfigModel map(SecurityConfigEntity entity);

    SecurityConfigEntity map(SecurityConfigModel model);
}
