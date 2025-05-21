package org.pancakelab.model.interfaces;

import org.pancakelab.model.enums.Ingredient;

import java.util.Set;
import java.util.UUID;

public interface Pancake {
    UUID getOrderId();
    UUID getPancakeId();
    String getDescription();
    Set<Ingredient> getIngredients();
}