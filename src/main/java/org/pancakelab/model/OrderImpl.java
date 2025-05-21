package org.pancakelab.model;

import org.pancakelab.model.enums.OrderStatus;
import org.pancakelab.model.interfaces.Order;

import java.util.UUID;

final class OrderImpl implements Order {

    private final UUID id;
    private final int building;
    private final int room;
    private volatile OrderStatus status;

    OrderImpl(int building, int room) {
        this.id = UUID.randomUUID();
        this.building = validateBuilding(building);
        this.room = validateRoom(room);
        this.status = OrderStatus.NEW;
    }

    @Override
    public synchronized void complete() {
        validateStatus(OrderStatus.NEW);
        status = OrderStatus.COMPLETED;
    }

    @Override
    public synchronized void prepare() {
        validateStatus(OrderStatus.COMPLETED);
        status = OrderStatus.PREPARING;
    }

    @Override
    public synchronized void deliver() {
        validateStatus(OrderStatus.PREPARING);
        status = OrderStatus.DELIVERED;
    }

    @Override
    public synchronized void cancel() {
        if (status != OrderStatus.NEW && status != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Can only cancel either NEW or COMPLETED orders");
        }
        status = OrderStatus.CANCELLED;
    }

    @Override public UUID getId() { return id; }
    @Override public OrderStatus getStatus() { return status; }
    @Override public int getBuilding() { return building; }
    @Override public int getRoom() { return room; }


    private void validateStatus(OrderStatus required) {
        if (status != required) {
            throw new IllegalStateException(
                    String.format("Order must be %s (current: %s)", required, status));
        }
    }

    private static int validateBuilding(int value) {
        if (value < 1 || value > 10) {
            throw new IllegalArgumentException("Building must be between 1 and 10");
        }
        return value;
    }

    private static int validateRoom(int value) {
        if (value < 1 || value > 999) {
            throw new IllegalArgumentException("Room must be between 1 and 999");
        }
        return value;
    }
}
