package com.mybudget.server.controllers;

import com.mybudget.server.dto.groups.AddCurrencyRequest;
import com.mybudget.server.dto.groups.CreateGroupRequest;
import com.mybudget.server.dto.groups.GroupNameRequest;
import com.mybudget.server.dto.groups.GroupResponse;
import com.mybudget.server.dto.groups.MoveAccountRequest;
import com.mybudget.server.services.groups.AccountGroupService;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Baskets of accounts (one account per currency)
@RestController
@RequestMapping("/api/user/groups")
@RequiredArgsConstructor
public class AccountGroupController {

    private final AccountGroupService groupService;
    private final UserUtils userUtils;

    // baskets and single accounts, as the accounts page shows them
    @GetMapping
    public ResponseEntity<List<GroupResponse>> list() {
        return ResponseEntity.ok(groupService.list(userUtils.getCurrentAuthenticatedUser()));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> get(@PathVariable String groupId) {
        return ResponseEntity.ok(groupService.get(groupId, userUtils.getCurrentAuthenticatedUser()));
    }

    // body: { "name", "type", "currency", "openingBalance" }
    @PostMapping
    public ResponseEntity<GroupResponse> create(@RequestBody CreateGroupRequest request) {
        GroupResponse created = groupService.create(request, userUtils.getCurrentAuthenticatedUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // body: { "name" }
    @PatchMapping("/{groupId}")
    public ResponseEntity<GroupResponse> rename(@PathVariable String groupId, @RequestBody GroupNameRequest request) {
        return ResponseEntity.ok(groupService.rename(groupId, request.getName(), userUtils.getCurrentAuthenticatedUser()));
    }

    // body: { "currency", "openingBalance" }
    @PostMapping("/{groupId}/currencies")
    public ResponseEntity<GroupResponse> addCurrency(@PathVariable String groupId,
                                                     @RequestBody AddCurrencyRequest request) {
        GroupResponse group = groupService.addCurrency(groupId, request, userUtils.getCurrentAuthenticatedUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    // a second currency for an account without basket: the basket is created around it
    @PostMapping("/from-account/{accountId}/currencies")
    public ResponseEntity<GroupResponse> addCurrencyToAccount(@PathVariable String accountId,
                                                              @RequestBody AddCurrencyRequest request) {
        GroupResponse group = groupService.addCurrencyToAccount(accountId, request, userUtils.getCurrentAuthenticatedUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    // body: { "groupId": "..." } to put an account into a basket, or { "groupId": null } to take it out
    @PatchMapping("/move/{accountId}")
    public ResponseEntity<List<GroupResponse>> moveAccount(@PathVariable String accountId,
                                                           @RequestBody MoveAccountRequest request) {
        return ResponseEntity.ok(groupService.moveAccount(accountId, request.getGroupId(),
                userUtils.getCurrentAuthenticatedUser()));
    }
}
