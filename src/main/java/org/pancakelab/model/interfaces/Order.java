package org.pancakelab.model.interfaces;

import org.pancakelab.model.enums.OrderStatus;

import java.util.UUID;

public interface Order {
    UUID getId();
    OrderStatus getStatus();
    int getBuilding();
    int getRoom();
    void complete();
    void prepare();
    void deliver();
    void cancel();
}
