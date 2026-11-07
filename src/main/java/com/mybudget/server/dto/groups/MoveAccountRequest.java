package com.mybudget.server.dto.groups;

import lombok.Data;

// groupId null: take the account out of its basket
@Data
public class MoveAccountRequest {
    private String groupId;
}
