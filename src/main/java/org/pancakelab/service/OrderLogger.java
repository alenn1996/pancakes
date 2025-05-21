package org.pancakelab.service;



import org.pancakelab.model.interfaces.Order;
import org.pancakelab.model.interfaces.Pancake;
import org.pancakelab.service.interfaces.Logger;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class OrderLogger implements Logger {
    private static volatile OrderLogger instance;
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();

    private OrderLogger() {}

    public static OrderLogger getInstance() {
        if (instance == null) {
            synchronized (OrderLogger.class) {
                if (instance == null) {
                    instance = new OrderLogger();
                }
            }
        }
        return instance;
    }

    @Override
    public void logOrderCreated(Order order) {
        log("[CREATE] Order %s for building %d room %d".formatted(
                order.getId(), order.getBuilding(), order.getRoom()));
    }

    @Override
    public void logPancakeAdded(UUID orderId, Pancake pancake) {
        log("[ADD] %s to order %s".formatted(
                pancake.getDescription(), orderId));
    }

    @Override
    public void logPancakeRemoved(UUID orderId, Pancake pancake) {
        log("[REMOVE] %s from order %s".formatted(
                pancake.getDescription(), orderId));
    }

    @Override
    public void logOrderStatusChange(Order order, String action) {
        log("[STATUS] Order %s %s → %s".formatted(
                order.getId(), action, order.getStatus()));
    }

    @Override
    public void logOrderDelivered(Order order) {
        log("[DELIVER] Order %s".formatted(order.getId()));
    }

    @Override
    public void logInvalidTransition(Order order, String action) {
        log("[ERROR] Invalid %s for order %s (current: %s)".formatted(
                action, order.getId(), order.getStatus()));
    }

    private void log(String message) {
        logQueue.add("[%s] %s".formatted(Instant.now(), message));
    }

    @Override
    public String getLastLog() {
        return logQueue.peek();
    }

    @Override
    public void clearLogs() {
        logQueue.clear();
    }
}