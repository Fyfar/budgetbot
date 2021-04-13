package com.home.budgetbot.bank.event;

import com.home.budgetbot.bank.client.AccountDto;
import com.home.budgetbot.bank.client.ClientInfoDto;
import com.home.budgetbot.bank.client.MonobankClient;
import com.home.budgetbot.bank.config.MonobankSecretProperties;
import com.home.budgetbot.bank.service.BankService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Stream;

@Log4j2
@Service
public class BalanceScheduler {

    @Autowired
    private BankService bankService;

    @Autowired
    private MonobankClient monobankClient;

    @Autowired
    private MonobankSecretProperties secretProperties;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Scheduled(initialDelayString = "${monobank.scheduler-delay}", fixedDelayString = "${monobank.scheduler-delay}")
    public void checkBalanceChange() {
        secretProperties.getTokenList()
                .stream()
                .peek(property -> log.info("Load client info for: {}.....", property.substring(0, 5)))
                .flatMap(this::loadClientInfo)
                .flatMap(this::findBalanceChanges)
                .peek(event -> log.info("Account with id: {} have balance change: {} -> {}", event.getAccountId(), event.getOldBalance(), event.getNewBalance()))
                .forEach(eventPublisher::publishEvent);
    }

    private Stream<BalanceChangeEvent> findBalanceChanges(ClientInfoDto clientInfo) {
        return clientInfo.getAccounts().stream()
                .flatMap(this::findBalanceChange);
    }

    private Stream<BalanceChangeEvent> findBalanceChange(AccountDto account) {
        String accountId = account.getId();
        account.setBalance(account.getBalance() / 100);

        Optional<Integer> lastBalance = bankService.findLastBalance(accountId);

        BalanceChangeEvent changeEvent = new BalanceChangeEvent()
                .setNewBalance(account.getBalance())
                .setAccountId(accountId);

        if (lastBalance.isPresent() && lastBalance.get() == account.getBalance()) {
            return Stream.empty();
        }

        if (lastBalance.isPresent() && lastBalance.get() != account.getBalance()) {
            changeEvent.setOldBalance(lastBalance.get());
        }

        bankService.saveToHistory(accountId, account.getBalance(), account.getBalance() % 100);

        return Stream.of(changeEvent);
    }

    private Stream<ClientInfoDto> loadClientInfo(String token) {
        try {
            ClientInfoDto response = monobankClient.getClientInfo(token);
            return Stream.of(response);
        } catch (Exception e) {
            eventPublisher.publishEvent(new ClientInfoFailEvent());
            log.error("Error while load client info", e);
            return Stream.empty();
        }
    }
}
