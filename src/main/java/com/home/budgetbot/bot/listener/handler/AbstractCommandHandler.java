package com.home.budgetbot.bot.listener.handler;

import lombok.Getter;

@Getter
public abstract class AbstractCommandHandler extends AbstractUpdateWrapperHandler {
    private String command;
    private String description;
    protected boolean hideCommand = false;

    public AbstractCommandHandler(String command, String description) {
        this.command = command;
        this.description = description;
    }

    @Override
    public boolean isSupport(UpdateWrapper wrapper) {
        return wrapper.getText()
                .filter(text -> command.equals(text))
                .isPresent();
    }
}
