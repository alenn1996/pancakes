package org.pancakelab.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
//used records for small immutble data that just return information
public record OrderDTO(UUID id, int building, int room, String status, List<PancakeDTO> pancakes) {
    public OrderDTO {
        Objects.requireNonNull(id, "Order ID cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(pancakes, "Pancakes cannot be null");
        pancakes = List.copyOf(pancakes);
    }
}