package com.home.budgetbot.bank;

import com.home.budgetbot.bank.event.BalanceChangeEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class BalanceChangeEventListener {

    private List<BalanceChangeEvent> eventList = new ArrayList<>();

    @EventListener
    public void onBalanceChange(BalanceChangeEvent event) {
        eventList.add(event);
    }

    public void clean() {
        eventList = new ArrayList<>();
    }

    public List<BalanceChangeEvent> getEventList() {
        return eventList;
    }
}
