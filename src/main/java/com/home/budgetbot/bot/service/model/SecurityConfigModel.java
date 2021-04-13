package com.home.budgetbot.bot.service.model;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@ToString
public class SecurityConfigModel {
    private List<Integer> authorizedUserList = new ArrayList<>();
}
