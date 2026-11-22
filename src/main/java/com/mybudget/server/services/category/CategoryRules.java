package com.mybudget.server.services.category;

// What a category name and emoji may be.
public final class CategoryRules {

    public static final int MAX_NAME = 40;
    // an emoji can be several characters (skin tone, flags, joined emojis)
    public static final int MAX_EMOJI = 16;
    public static final String DEFAULT_EMOJI = "🗂️";

    private CategoryRules() {
    }

    // trimmed name; null when empty
    public static String name(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim().replaceAll("\\s+", " ");
        if (trimmed.length() > MAX_NAME) {
            throw new IllegalArgumentException("The category name can have at most " + MAX_NAME + " characters");
        }
        return trimmed;
    }

    // trimmed emoji; the default one when empty
    public static String emoji(String emoji) {
        if (emoji == null || emoji.isBlank()) {
            return DEFAULT_EMOJI;
        }
        String trimmed = emoji.trim();
        if (trimmed.length() > MAX_EMOJI) {
            throw new IllegalArgumentException("The emoji is too long");
        }
        return trimmed;
    }
}