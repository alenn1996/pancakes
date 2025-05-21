package org.pancakelab.model;

import org.pancakelab.model.enums.Ingredient;
import org.pancakelab.model.interfaces.Pancake;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PancakeFactory {
    // Single shared Builder instance
    //we could have went with approach to create a single builder on each create Pancake call but i decided for this scenario
    private static final PancakeImpl.Builder BUILDER = new PancakeImpl.Builder();
    private PancakeFactory() {}

    public static Pancake createPancake(UUID orderId, List<Ingredient> ingredients)  {
        // Reset the Builder state
        BUILDER.reset();
        BUILDER.setOrderId(orderId);
        for (Ingredient ingredient : ingredients) {
            BUILDER.addIngredient(ingredient);
        }
        return BUILDER.build();
    }



    public static boolean isValidPancake(Pancake pancake, List<String> ingredientNames) {
        Set<Ingredient> pancakeIngredients = pancake.getIngredients();
        Set<String> pancakeIngredientNames = pancakeIngredients.stream()
                .map(Ingredient::displayName)
                .collect(Collectors.toSet());
        Set<String> inputIngredientNames = new HashSet<>(ingredientNames);
        return pancakeIngredientNames.equals(inputIngredientNames);
    }
}
