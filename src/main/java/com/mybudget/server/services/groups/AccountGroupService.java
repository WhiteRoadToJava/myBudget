package com.mybudget.server.services.groups;

import com.mybudget.server.dto.accounts.AccountResponse;
import com.mybudget.server.dto.groups.AddCurrencyRequest;
import com.mybudget.server.dto.groups.CreateGroupRequest;
import com.mybudget.server.dto.groups.GroupResponse;
import com.mybudget.server.exeptions.ConflictException;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.mapMappers.AccountMapper;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.AccountGroup;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.AccountGroupRepository;
import com.mybudget.server.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Baskets: one account per currency, shown as one account.
 *
 * Rules:
 * - one account per currency in a basket;
 * - an account in a basket is named "<basket> · <CUR>", so names stay unique and every
 *   list, statement and report that shows account names keeps working unchanged;
 * - an account without a basket is shown as a basket of one currency (no migration);
 * - a basket left without accounts is removed.
 */
@Service
@RequiredArgsConstructor
public class AccountGroupService {

    private static final String SEPARATOR = " · ";

    private final AccountGroupRepository groupRepository;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    // ---------- read ----------

    // the user's baskets, then the accounts without one; each basket's currencies by name
    public List<GroupResponse> list(User user) {
        List<Account> accounts = accountRepository.findAllByUser(user);
        Map<String, List<Account>> byGroup = accounts.stream()
                .filter(a -> a.getGroupId() != null)
                .collect(Collectors.groupingBy(Account::getGroupId));

        List<GroupResponse> result = new ArrayList<>();
        for (AccountGroup group : groupRepository.findAllByUser(user)) {
            List<Account> members = byGroup.getOrDefault(group.getId(), List.of());
            result.add(toResponse(group, members));
        }
        accounts.stream()
                .filter(a -> a.getGroupId() == null)
                .forEach(a -> result.add(new GroupResponse(null, a.getName(), a.getType(), true,
                        List.of(accountMapper.mapToAccountResponse(a)))));
        result.sort(Comparator.comparing(g -> g.name() == null ? "" : g.name().toLowerCase(Locale.ROOT)));
        return result;
    }

    public GroupResponse get(String groupId, User user) {
        AccountGroup group = findOwnedGroup(groupId, user);
        return toResponse(group, accountRepository.findAllByGroupId(group.getId()));
    }

    // ---------- create ----------

    public GroupResponse create(CreateGroupRequest request, User user) {
        String name = requireName(request.getName());
        String currency = requireCurrency(request.getCurrency());
        ensureNameFree(name, user, null);

        AccountGroup group = new AccountGroup();
        group.setName(name);
        group.setType(request.getType());
        group.setUser(user);
        group.setCreatedAt(Instant.now());
        group = groupRepository.save(group);

        newAccount(group, currency, request.getOpeningBalance(), user);
        return get(group.getId(), user);
    }

    public GroupResponse addCurrency(String groupId, AddCurrencyRequest request, User user) {
        AccountGroup group = findOwnedGroup(groupId, user);
        String currency = requireCurrency(request.getCurrency());
        ensureCurrencyFree(group.getId(), currency);
        newAccount(group, currency, request.getOpeningBalance(), user);
        return get(group.getId(), user);
    }

    // A second currency for an account that has no basket yet: the basket is created
    // around it, named like the account, and the account keeps all its transactions.
    public GroupResponse addCurrencyToAccount(String accountId, AddCurrencyRequest request, User user) {
        Account account = findOwnedAccount(accountId, user);
        if (account.getGroupId() != null) {
            return addCurrency(account.getGroupId(), request, user);
        }
        String currency = requireCurrency(request.getCurrency());
        if (currency.equalsIgnoreCase(account.getCurrency())) {
            throw new ConflictException("This currency is already in the account");
        }

        AccountGroup group = new AccountGroup();
        group.setName(account.getName());
        group.setType(account.getType());
        group.setUser(user);
        group.setCreatedAt(Instant.now());
        group = groupRepository.save(group);

        account.setGroupId(group.getId());
        account.setName(label(group.getName(), account.getCurrency()));
        accountRepository.save(account);

        newAccount(group, currency, request.getOpeningBalance(), user);
        return get(group.getId(), user);
    }

    // ---------- change ----------

    // renames the basket and every account in it
    public GroupResponse rename(String groupId, String newName, User user) {
        AccountGroup group = findOwnedGroup(groupId, user);
        String name = requireName(newName);
        ensureNameFree(name, user, group.getId());

        group.setName(name);
        groupRepository.save(group);
        List<Account> members = accountRepository.findAllByGroupId(group.getId());
        for (Account account : members) {
            account.setName(label(name, account.getCurrency()));
        }
        accountRepository.saveAll(members);
        return get(group.getId(), user);
    }

    // Puts an existing account into a basket (merging old accounts), or takes it out
    // (groupId null). The account keeps its transactions; only its basket and name change.
    public List<GroupResponse> moveAccount(String accountId, String targetGroupId, User user) {
        Account account = findOwnedAccount(accountId, user);
        String previousGroupId = account.getGroupId();

        if (targetGroupId == null || targetGroupId.isBlank()) {
            account.setGroupId(null);
        } else {
            AccountGroup target = findOwnedGroup(targetGroupId, user);
            if (target.getId().equals(previousGroupId)) {
                return list(user);
            }
            ensureCurrencyFree(target.getId(), account.getCurrency());
            account.setGroupId(target.getId());
            account.setName(label(target.getName(), account.getCurrency()));
        }
        accountRepository.save(account);
        removeIfEmpty(previousGroupId);
        return list(user);
    }

    // ---------- helpers ----------

    private void newAccount(AccountGroup group, String currency, Double openingBalance, User user) {
        double opening = openingBalance == null ? 0.0 : openingBalance;
        Account account = new Account(
                label(group.getName(), currency),
                null,
                opening,
                opening,
                currency,
                group.getType(),
                user,
                new Date().toString());
        account.setGroupId(group.getId());
        accountRepository.save(account);
    }

    private void removeIfEmpty(String groupId) {
        if (groupId != null && accountRepository.findAllByGroupId(groupId).isEmpty()) {
            groupRepository.deleteById(groupId);
        }
    }

    private GroupResponse toResponse(AccountGroup group, List<Account> members) {
        List<AccountResponse> accounts = members.stream()
                .sorted(Comparator.comparing(a -> a.getCurrency() == null ? "" : a.getCurrency()))
                .map(accountMapper::mapToAccountResponse)
                .toList();
        return new GroupResponse(group.getId(), group.getName(), group.getType(), false, accounts);
    }

    static String label(String groupName, String currency) {
        return groupName + SEPARATOR + currency;
    }

    // a basket name must not be used by another basket or by an account without basket
    private void ensureNameFree(String name, User user, String exceptGroupId) {
        String wanted = name.toLowerCase(Locale.ROOT);
        boolean usedByGroup = groupRepository.findAllByUser(user).stream()
                .anyMatch(g -> !g.getId().equals(exceptGroupId) && g.getName() != null
                        && g.getName().toLowerCase(Locale.ROOT).equals(wanted));
        boolean usedByAccount = accountRepository.findAllByUser(user).stream()
                .anyMatch(a -> a.getGroupId() == null && a.getName() != null
                        && a.getName().toLowerCase(Locale.ROOT).equals(wanted));
        if (usedByGroup || usedByAccount) {
            throw new ConflictException("You already have an account with this name");
        }
    }

    private void ensureCurrencyFree(String groupId, String currency) {
        boolean taken = accountRepository.findAllByGroupId(groupId).stream()
                .anyMatch(a -> currency.equalsIgnoreCase(a.getCurrency()));
        if (taken) {
            throw new ConflictException("This currency is already in the account");
        }
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (name.contains(SEPARATOR.trim())) {
            throw new IllegalArgumentException("The name can not contain \"·\"");
        }
        return name.trim();
    }

    private String requireCurrency(String currency) {
        if (currency == null || !currency.trim().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("Choose a currency");
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private AccountGroup findOwnedGroup(String groupId, User user) {
        AccountGroup group = groupId == null ? null : groupRepository.findByIdAndUser(groupId, user);
        if (group == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        return group;
    }

    private Account findOwnedAccount(String accountId, User user) {
        Account account = accountId == null ? null : accountRepository.findByIdAndUser(accountId, user);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        return account;
    }
}
