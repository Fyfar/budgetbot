package com.home.budgetbot.bot.service.mapper;

import com.home.budgetbot.bot.repository.entity.UserEntity;
import com.home.budgetbot.bot.service.model.UserModel;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserModel map(UserEntity entity);
    UserEntity map(UserModel model);
    List<UserModel> mapEntityList(List<UserEntity> entity);
    List<UserEntity> mapModelList(List<UserModel> model);


}
