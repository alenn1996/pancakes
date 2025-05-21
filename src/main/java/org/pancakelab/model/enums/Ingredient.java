package org.pancakelab.model.enums;

import java.util.HashMap;
import java.util.Map;

// Enum for valid ingredients (package-private)
public enum Ingredient {
    DARK_CHOCOLATE("dark chocolate"),
    MILK_CHOCOLATE("milk chocolate"),
    WHIPPED_CREAM("whipped cream"),
    HAZELNUTS("hazelnuts");

    private final String displayName;

    Ingredient(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    private static final Map<String, Ingredient> BY_NAME = new HashMap<>();

    static {
        for (Ingredient ingredient : values()) {
            BY_NAME.put(ingredient.displayName.toLowerCase(), ingredient);
        }
    }

    public static Ingredient fromName(String name) {
        Ingredient ingredient = BY_NAME.get(name.toLowerCase());
        if (ingredient == null) {
            throw new IllegalArgumentException("Unknown ingredient: " + name);
        }
        return ingredient;
    }
}
