package org.pancakelab.model;

import org.pancakelab.model.enums.Ingredient;
import org.pancakelab.model.interfaces.Pancake;

import java.util.*;
import java.util.stream.Collectors;

final class PancakeImpl implements Pancake {
    private final UUID orderId;
    private final UUID pancakeId;
    private final Set<Ingredient> ingredients;


    static final class Builder {
        private UUID orderId;
        //thread safety
        private final Set<Ingredient> ingredients = Collections.synchronizedSet(new LinkedHashSet<>());

        Builder() {
            // Default constructor
        }

        void setOrderId(UUID orderId) {
            if (this.orderId != null) {
                throw new IllegalStateException("orderId has already been set to " + this.orderId + "; cannot set it again to " + orderId);
            }
            this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        }

        //to keep following builder pattern we return Builder
        Builder addIngredient(Ingredient ingredient) {
            Objects.requireNonNull(ingredient, "Ingredient cannot be null");
            ingredients.add(ingredient);
            return this;
        }

        void reset() {
            orderId = null; // Reset orderId
            ingredients.clear();
        }


        Pancake build() {
            if (ingredients.isEmpty()) {
                throw new IllegalStateException("Pancake must have at least one ingredient");
            }
            return new PancakeImpl(this);
        }
    }

    private PancakeImpl(Builder builder) {
        this.orderId = builder.orderId;
        this.pancakeId = UUID.randomUUID();
        this.ingredients = Collections.unmodifiableSet(new LinkedHashSet<>(builder.ingredients));
    }

    @Override public UUID getOrderId() { return orderId; }
    @Override public UUID getPancakeId() { return pancakeId; }
    @Override public String getDescription() {
        return ingredients.isEmpty()
                ? "Plain pancake"
                : "Delicious pancake with " + ingredients.stream()
                .map(Ingredient::displayName)
                .collect(Collectors.joining(", "))
                + "!";
    }
    @Override public Set<Ingredient> getIngredients() { return ingredients; }
}