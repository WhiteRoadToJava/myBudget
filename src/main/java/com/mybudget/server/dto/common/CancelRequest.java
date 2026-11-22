package com.mybudget.server.dto.common;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CancelRequest {
    private String reason;

    // the day it was cancelled; empty = now
    private LocalDateTime cancelledAt;
}
