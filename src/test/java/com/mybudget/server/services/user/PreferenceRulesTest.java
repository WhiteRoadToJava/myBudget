package com.mybudget.server.services.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PreferenceRulesTest {

    @Test
    void emptyClearsAChoice() {
        assertNull(PreferenceRules.language(""));
        assertNull(PreferenceRules.currency("  "));
        assertNull(PreferenceRules.firstDayOfWeek(""));
        assertNull(PreferenceRules.amountDisplay(""));
    }

    @Test
    void valuesAreTidied() {
        assertEquals("ar", PreferenceRules.language(" AR "));
        assertEquals("SEK", PreferenceRules.currency("sek"));
        assertEquals("SATURDAY", PreferenceRules.firstDayOfWeek("saturday"));
        assertEquals("SYMBOL", PreferenceRules.amountDisplay(" symbol "));   // valuesAreTidied

    }

    @Test
    void wrongValuesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> PreferenceRules.language("fr"));
        assertThrows(IllegalArgumentException.class, () -> PreferenceRules.currency("SEKK"));
        assertThrows(IllegalArgumentException.class, () -> PreferenceRules.currency("12A"));
        assertThrows(IllegalArgumentException.class, () -> PreferenceRules.firstDayOfWeek("FRIDAY"));
        assertThrows(IllegalArgumentException.class, () -> PreferenceRules.amountDisplay("FLAG")); // wrongValuesAreRefused
    }
}