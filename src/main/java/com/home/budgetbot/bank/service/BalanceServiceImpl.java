package com.home.budgetbot.bank.service;

import static com.home.budgetbot.bank.BalanceConverter.balanceToUAH;

import com.home.budgetbot.bank.event.BalanceChangeEvent;
import com.home.budgetbot.bank.model.BalanceChangedWebhookInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class BalanceServiceImpl implements BalanceService {

    private final BankService bankService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void balanceChanged(BalanceChangedWebhookInput input) {
        String accountId = input.getAccountData().getAccount();
        Optional<Integer> lastBalance = bankService.findLastBalance(accountId);

        int newBalance = balanceToUAH(input.getBalanceChangedEvent().getBalance()).intValue();
        BalanceChangeEvent changeEvent = new BalanceChangeEvent()
                .setNewBalance(newBalance)
                .setAccountId(accountId);

        if (lastBalance.isPresent() && lastBalance.get() == newBalance) {
            log.info("Balance wasn't changed from the last operation");
            return;
        }

        lastBalance.ifPresent(changeEvent::setOldBalance);
        bankService.saveToHistory(accountId, newBalance, newBalance % 100);
        log.info("Account with id: {} have balance change: {} -> {}", accountId, changeEvent.getOldBalance(), changeEvent.getNewBalance());
        eventPublisher.publishEvent(changeEvent);
    }
}
