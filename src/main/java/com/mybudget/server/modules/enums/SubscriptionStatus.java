package com.mybudget.server.modules.enums;

// Computed from the dates, never stored
public enum SubscriptionStatus {
    NONE,     // no end date yet (users from before subscriptions): full access until an admin sets one
    TRIAL,    // free trial, full access
    ACTIVE,   // paid, full access
    GRACE,    // ended a few days ago: full access, the app asks to renew
    EXPIRED   // read only
}
