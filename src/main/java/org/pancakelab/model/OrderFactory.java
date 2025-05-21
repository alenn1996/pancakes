package org.pancakelab.model;

import org.pancakelab.model.interfaces.Order;

public final class OrderFactory {
    private OrderFactory() {}

    public static Order createOrder(int building, int room) {
        return new OrderImpl(building, room);
    }
}
