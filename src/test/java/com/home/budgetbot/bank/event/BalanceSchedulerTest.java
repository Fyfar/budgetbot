package com.home.budgetbot.bank.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.home.budgetbot.bank.BalanceChangeEventListener;
import com.home.budgetbot.bank.client.AccountDto;
import com.home.budgetbot.bank.client.ClientInfoDto;
import com.home.budgetbot.bank.repository.BalanceHistoryRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("integration")
class BalanceSchedulerTest {
    public static final String ACCOUNT_ID = "q2esff254";

    @Autowired
    private BalanceScheduler balanceScheduler;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private BalanceHistoryRepository historyRepository;

    @Autowired
    private BalanceChangeEventListener eventListener;

    @BeforeAll
    static void beforeAll() {
        WireMockServer wireMockServer = new WireMockServer(8080);
        wireMockServer.start();
    }

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        eventListener.clean();
    }

    @SneakyThrows
    private void mockClientInfo(String accountId, int balance) {
        AccountDto accountDto = new AccountDto()
                .setId(accountId)
                .setBalance(balance);

        ClientInfoDto clientInfoDto = new ClientInfoDto()
                .setAccounts(List.of(accountDto));

        String response = objectMapper.writeValueAsString(clientInfoDto);

        stubFor(get(urlEqualTo("/personal/client-info"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    @Test
    void shouldFireEventWithNullOldValue() {
        mockClientInfo(ACCOUNT_ID, 10023);

        balanceScheduler.checkBalanceChange();

        List<BalanceChangeEvent> eventList = eventListener.getEventList();

        assertEquals(1, eventList.size());
        assertEquals(100, eventList.get(0).getNewBalance());
        assertNull(eventList.get(0).getOldBalance());
        assertNotNull(eventList.get(0).getAccountId());
    }

    @Test
    void shouldNotFireEventWhenNoChanges() {
        mockClientInfo(ACCOUNT_ID, 10023);

        balanceScheduler.checkBalanceChange();

        mockClientInfo(ACCOUNT_ID, 10023);

        balanceScheduler.checkBalanceChange();

        List<BalanceChangeEvent> eventList = eventListener.getEventList();

        assertEquals(1, eventList.size());
        assertEquals(100, eventList.get(0).getNewBalance());
        assertNull(eventList.get(0).getOldBalance());
        assertNotNull(eventList.get(0).getAccountId());
    }

    @Test
    void shouldHaveOldValue() {
        mockClientInfo(ACCOUNT_ID, 10023);

        balanceScheduler.checkBalanceChange();

        mockClientInfo(ACCOUNT_ID, 20023);

        balanceScheduler.checkBalanceChange();

        List<BalanceChangeEvent> eventList = eventListener.getEventList();

        assertEquals(2, eventList.size());
        assertEquals(200, eventList.get(1).getNewBalance());
        assertEquals(100, eventList.get(1).getOldBalance());
        assertNotNull(eventList.get(0).getAccountId());
    }
}