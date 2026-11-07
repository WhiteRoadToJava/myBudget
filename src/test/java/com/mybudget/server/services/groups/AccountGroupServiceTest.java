package com.mybudget.server.services.groups;

import com.mybudget.server.dto.groups.AddCurrencyRequest;
import com.mybudget.server.dto.groups.CreateGroupRequest;
import com.mybudget.server.exeptions.ConflictException;
import com.mybudget.server.mapMappers.AccountMapper;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.AccountGroup;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.AccountGroupRepository;
import com.mybudget.server.repositories.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountGroupServiceTest {

    private AccountGroupRepository groups;
    private AccountRepository accounts;
    private AccountGroupService service;
    private final User user = new User();
    private final List<Account> stored = new ArrayList<>();

    @BeforeEach
    void setUp() {
        groups = mock(AccountGroupRepository.class);
        accounts = mock(AccountRepository.class);
        service = new AccountGroupService(groups, accounts, new AccountMapper());
        user.setId("u1");
        when(groups.save(any())).thenAnswer(i -> {
            AccountGroup g = i.getArgument(0);
            if (g.getId() == null) g.setId("g1");
            return g;
        });
        when(accounts.save(any())).thenAnswer(i -> {
            Account a = i.getArgument(0);
            if (!stored.contains(a)) stored.add(a);
            return a;
        });
        when(accounts.findAllByUser(user)).thenAnswer(i -> stored);
        when(accounts.findAllByGroupId(any())).thenAnswer(i ->
                stored.stream().filter(a -> i.getArgument(0).equals(a.getGroupId())).toList());
    }

    private AccountGroup basket(String name) {
        AccountGroup g = new AccountGroup();
        g.setId("g1");
        g.setName(name);
        g.setUser(user);
        when(groups.findByIdAndUser("g1", user)).thenReturn(g);
        when(groups.findAllByUser(user)).thenReturn(List.of(g));
        return g;
    }

    private Account account(String id, String name, String currency, String groupId) {
        Account a = new Account();
        a.setId(id);
        a.setName(name);
        a.setCurrency(currency);
        a.setGroupId(groupId);
        a.setUser(user);
        stored.add(a);
        when(accounts.findByIdAndUser(id, user)).thenReturn(a);
        return a;
    }

    @Test
    void aNewBasketGetsItsFirstCurrency() {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName(" Wallet ");
        request.setCurrency("sek");
        request.setOpeningBalance(100.0);
        when(groups.findByIdAndUser("g1", user)).thenAnswer(i -> {
            AccountGroup g = new AccountGroup();
            g.setId("g1");
            g.setName("Wallet");
            return g;
        });

        service.create(request, user);

        assertEquals(1, stored.size());
        assertEquals("Wallet · SEK", stored.get(0).getName());
        assertEquals("SEK", stored.get(0).getCurrency());
        assertEquals("g1", stored.get(0).getGroupId());
        assertEquals(100.0, stored.get(0).getTotalBalance());
    }

    @Test
    void aCurrencyAppearsOnlyOncePerBasket() {
        basket("Wallet");
        account("a1", "Wallet · SEK", "SEK", "g1");
        AddCurrencyRequest request = new AddCurrencyRequest();
        request.setCurrency("SEK");
        assertThrows(ConflictException.class, () -> service.addCurrency("g1", request, user));
    }

    @Test
    void aSecondCurrencyWrapsASingleAccountInABasket() {
        Account main = account("a1", "Main", "SEK", null);
        when(groups.findByIdAndUser("g1", user)).thenAnswer(i -> {
            AccountGroup g = new AccountGroup();
            g.setId("g1");
            g.setName("Main");
            return g;
        });
        AddCurrencyRequest request = new AddCurrencyRequest();
        request.setCurrency("USD");

        service.addCurrencyToAccount("a1", request, user);

        assertEquals("g1", main.getGroupId());
        assertEquals("Main · SEK", main.getName());
        assertEquals("Main · USD", stored.get(1).getName());
    }

    @Test
    void renamingABasketRenamesItsAccounts() {
        basket("Wallet");
        Account sek = account("a1", "Wallet · SEK", "SEK", "g1");
        Account usd = account("a2", "Wallet · USD", "USD", "g1");
        service.rename("g1", "Family", user);
        assertEquals("Family · SEK", sek.getName());
        assertEquals("Family · USD", usd.getName());
    }

    @Test
    void movingAnAccountIntoABasketWithItsCurrencyIsRefused() {
        basket("Wallet");
        account("a1", "Wallet · SEK", "SEK", "g1");
        account("a2", "Old", "SEK", null);
        assertThrows(ConflictException.class, () -> service.moveAccount("a2", "g1", user));
    }

    @Test
    void theLastAccountLeavingABasketRemovesIt() {
        basket("Wallet");
        Account sek = account("a1", "Wallet · SEK", "SEK", "g1");
        service.moveAccount("a1", null, user);
        assertEquals(null, sek.getGroupId());
        verify(groups, atLeastOnce()).deleteById("g1");
    }

    @Test
    void theSeparatorCanNotBeInAName() {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("A · B");
        request.setCurrency("SEK");
        assertThrows(IllegalArgumentException.class, () -> service.create(request, user));
        verify(groups, never()).save(any());
    }

    @Test
    void aBasketNameCanNotBeUsedTwice() {
        account("a9", "Savings", "EUR", null);
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("savings");
        request.setCurrency("SEK");
        assertThrows(ConflictException.class, () -> service.create(request, user));
    }
}
