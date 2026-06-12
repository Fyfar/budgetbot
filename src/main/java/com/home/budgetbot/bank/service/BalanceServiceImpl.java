package com.home.budgetbot.bank.service;

import static com.home.budgetbot.bank.BalanceConverter.balanceToUAH;

import com.home.budgetbot.bank.event.BalanceChangeEvent;
import com.home.budgetbot.bank.model.BalanceChangedWebhookInput;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class BalanceServiceImpl implements BalanceService {

    private final Map<String, Instant> seenTransactionIds = new ConcurrentHashMap<>();

    @Inject
    BankService bankService;

    @Inject
    ApplicationEventPublisher<BalanceChangeEvent> eventPublisher;

    @Override
    public void balanceChanged(BalanceChangedWebhookInput input) {
        if (input.getAccountData() == null || input.getAccountData().getStatementItem() == null) {
            log.warn("Ignoring webhook with missing data or statementItem (likely a Monobank ping)");
            return;
        }

        String accountId = input.getAccountData().getAccount();
        String txId = input.getAccountData().getStatementItem().getId();
        if (txId != null && isDuplicate(txId)) {
            log.info("Duplicate webhook for transaction {} on account {}, skipping", txId, accountId);
            return;
        }

        Optional<Integer> lastBalance = bankService.findLastBalance(accountId);

        int newBalance = balanceToUAH(input.getAccountData().getStatementItem().getBalance()).intValue();
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

    private boolean isDuplicate(String txId) {
        evictExpired();
        return seenTransactionIds.putIfAbsent(txId, Instant.now()) != null;
    }

    private void evictExpired() {
        Instant cutoff = Instant.now().minusSeconds(3600);
        Iterator<Map.Entry<String, Instant>> it = seenTransactionIds.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isBefore(cutoff)) {
                it.remove();
            }
        }
    }
}
