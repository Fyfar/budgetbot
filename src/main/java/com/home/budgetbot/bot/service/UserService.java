package com.home.budgetbot.bot.service;

import com.home.budgetbot.bot.repository.UserRepository;
import com.home.budgetbot.bot.repository.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

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
