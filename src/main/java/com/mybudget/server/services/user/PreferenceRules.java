package com.mybudget.server.services.user;


import java.util.Locale;
import java.util.Set;

public class PreferenceRules {
    public static final Set<String> LANGUAGES = Set.of("ar", "en", "sv");
    public static final Set<String> FIRST_DAYS = Set.of("SATURDAY", "SUNDAY", "MONDAY");

    public static String language(String value){
        String code = clean(value);
        if (code == null) return null;
        code = code.toLowerCase(Locale.ROOT);
        if (!LANGUAGES.contains(code)){
            throw new IllegalArgumentException("Unknown language: " + value);
        }
        return code;
    }

    public static String currency(String value) {
        String code = clean(value);
        if (code == null) return null;
        code = code.toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("A currency is a 3-letter code such as SEK");
        }
        return code;
    }

    public static String firstDayOfWeek(String value){
        String day = clean(value);
        if (day == null) return null;
        day = day.toUpperCase(Locale.ROOT);
        if (!FIRST_DAYS.contains(day)){
            throw new IllegalArgumentException("The week can start on Saturday, Sunday or Monday");
        }
        return day;
    }

    private static String clean(String value){
        return value == null || value.isBlank() ? null : value.trim();
    }
}
