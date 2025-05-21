package org.pancakelab.service.interfaces;

import org.pancakelab.model.interfaces.Order;
import org.pancakelab.model.interfaces.Pancake;

import java.util.UUID;

public interface Logger {
    void logOrderCreated(Order order);
    void logPancakeAdded(UUID orderId, Pancake pancake);
    void logPancakeRemoved(UUID orderId, Pancake pancake);
    void logOrderStatusChange(Order order, String action);
    void logOrderDelivered(Order order);
    void logInvalidTransition(Order order, String action);
    String getLastLog();
    void clearLogs();
}
