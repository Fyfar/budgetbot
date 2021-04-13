package com.home.budgetbot.bank;

import com.home.budgetbot.bank.event.BalanceChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
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
