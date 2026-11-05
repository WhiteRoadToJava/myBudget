package com.mybudget.server.modules.enums;

// Direction of a transfer fee on its account
public enum FeeDirection {
    CHARGE, // علينا: taken from the fee account
    CREDIT  // لنا: added to the fee account
}
