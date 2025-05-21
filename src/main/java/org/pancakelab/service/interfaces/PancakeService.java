package org.pancakelab.service.interfaces;

import org.pancakelab.dto.OrderDTO;
import org.pancakelab.dto.PancakeDTO;
import org.pancakelab.model.enums.OrderStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PancakeService {
    OrderDTO createOrder(int building, int room);
    void addPancakes(UUID orderId, List<String> ingredientNames, int quantity);
    void removePancake(UUID orderId, List<String> ingredientNames);
    void removePancake(UUID orderId, UUID pancakeId);
    void removePancakes(String description, UUID orderId, int quantity);
    void completeOrder(UUID orderId);
    void prepareOrder(UUID orderId);
    OrderDTO deliverOrder(UUID orderId);
    void cancelOrder(UUID orderId);
    void clearAllFinishedOrders();
    OrderDTO getOrderStatus(UUID orderId);
    List<PancakeDTO> getPancakeDescriptions(UUID orderId);
    List<String> viewOrder(UUID orderId);
    Set<UUID> listOrdersWithStatus(OrderStatus orderStatus);
}
