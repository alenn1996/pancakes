package org.pancakelab.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PancakeDTO(UUID orderId, UUID pancakeId, List<String> ingredients, String description) {
    public PancakeDTO {
        Objects.requireNonNull(ingredients, "Ingredients cannot be null");
        ingredients = List.copyOf(ingredients);
    }
}
