package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.UserRepository;
import com.home.budgetbot.bot.repository.entity.UserEntity;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class UserService {

    @Inject
    UserRepository userRepository;

    public boolean isNotExist(int userId) {
        return userRepository.findById(userId).isEmpty();
    }

    public void save(int id, String username, String firstName, String lastName, Long chatId) {
        UserEntity userEntity = new UserEntity().setId(id)
                .setUsername(username)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setChatId(chatId);

        userRepository.save(userEntity);
    }
}
