package org.pancakelab.service;


import org.pancakelab.dto.OrderDTO;
import org.pancakelab.dto.PancakeDTO;
import org.pancakelab.model.enums.Ingredient;
import org.pancakelab.model.enums.OrderStatus;
import org.pancakelab.model.OrderFactory;
import org.pancakelab.model.PancakeFactory;
import org.pancakelab.model.interfaces.Order;
import org.pancakelab.model.interfaces.Pancake;
import org.pancakelab.service.interfaces.Logger;
import org.pancakelab.service.interfaces.PancakeService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PancakeServiceImpl implements PancakeService {
    private final ConcurrentMap<UUID, Order> activeOrders = new ConcurrentHashMap<>(); // here we have active orders
    private final ConcurrentMap<UUID, List<Pancake>> orderPancakes = new ConcurrentHashMap<>(); // here we have pancakes
    private final ConcurrentMap<UUID, Order> finishedOrders = new ConcurrentHashMap<>();
    //finished orders were not requested but added in case if we want to review finished orders(cancelled or delivered)
    private final Logger logger; // logger

    // ================== Public API ================== //

    /**
     * constructor
     * @throws NullPointerException if logger is null
     */
    public PancakeServiceImpl(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
    }

    /**
     * Creates an order and returns the Order object to satisfy tests.
     *  @throws IllegalArgumentException if building/room numbers are invalid
     */
    @Override
    public OrderDTO createOrder(int building, int room) {
        Order order = OrderFactory.createOrder(building, room);
        UUID orderId = order.getId();
        activeOrders.put(orderId, order);
        // thread safe list
        orderPancakes.put(orderId, Collections.synchronizedList(new ArrayList<>()));
        logger.logOrderCreated(order);
        return new OrderDTO(
                order.getId(),
                order.getBuilding(),
                order.getRoom(),
                order.getStatus().name(),
                List.of() // No pancakes initially
        );
    }

    /**
     * add pancake to order id, since customers shall not be concerned about internal logic
     * they will add ingredient names and that's it no need to bother them with enums
     *  calls internal method
     */
    @Override
    public void addPancakes(UUID orderId, List<String> ingredientNames, int quantity) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (String ingredientName : ingredientNames) {
            ingredients.add(Ingredient.fromName(ingredientName));
        }
        addPancakesInternal(orderId, ingredients,quantity);
    }

    /*
     * remove pancake based on ingredient names
     * @throws IllegalStateException if state is not new
     * @throws IllegalArgumentException if ingredient does not exist
     */
    @Override
    public void removePancake(UUID orderId, List<String> ingredientNames) {
        validateOrderState(orderId, OrderStatus.NEW);

        orderPancakes.computeIfPresent(orderId, (id, pancakes) -> {
            Pancake toRemove = pancakes.stream()
                    .filter(p -> PancakeFactory.isValidPancake(p, ingredientNames))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Pancake with ingredients " + ingredientNames + " not found in order " + orderId));
            pancakes.remove(toRemove);
            logger.logPancakeRemoved(orderId, toRemove);
            return pancakes;
        });
    }

    /*
     * remove pancake from order based by pancake id
     * @throws IllegalStateException if state is not new
     * @throws IllegalArgumentException if ingredient does not exist
     */
    @Override
    public void removePancake(UUID orderId, UUID pancakeId) {
        validateOrderState(orderId, OrderStatus.NEW);

        orderPancakes.computeIfPresent(orderId, (id, pancakes) -> {
            Pancake toRemove = pancakes.stream()
                    .filter(p -> p.getPancakeId().equals(pancakeId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Pancake with ID " + pancakeId + " not found in order " + orderId));
            pancakes.remove(toRemove);
            logger.logPancakeRemoved(orderId, toRemove);
            return pancakes;
        });
    }

    /*
     * remove pancake from order based on description by orderid
     * @throws IllegalStateException if state is not new
     * @throws IllegalArgumentException if ingredient does not exist
     */
    @Override
    public void removePancakes(String description, UUID orderId, int quantity) {
        validateOrderState(orderId, OrderStatus.NEW);

        orderPancakes.computeIfPresent(orderId, (id, pancakes) -> {
            List<Pancake> toRemove = pancakes.stream()
                    .filter(p -> p.getDescription().equals(description))
                    .limit(quantity)
                    .toList();
            if (toRemove.size() < quantity) {
                throw new IllegalArgumentException(
                        String.format("Cannot remove %d pancakes of type %s; only %d available",
                                quantity, description, toRemove.size()));
            }
            pancakes.removeAll(toRemove);
            toRemove.forEach(p -> logger.logPancakeRemoved(orderId, p));
            return pancakes;
        });
    }

    /*
     * move order to status completed
     * @throws IllegalArgumentException if an order does not exist
     * @throws IllegalStateException if an order is in incorrect state or there are no pancakes
     */
    @Override
    public void completeOrder(UUID orderId) {
        validateOrderExists(orderId);
        List<Pancake> pancakes = orderPancakes.getOrDefault(orderId, Collections.emptyList());
        if (pancakes.isEmpty()) {
            throw new IllegalStateException("Cannot complete order " + orderId + " with no pancakes");
        }
        executeOrderAction(orderId, Order::complete, "Completed");
    }

    /*
     * move order to status prepare
     * @throws IllegalArgumentException if an order does not exist
     * @throws IllegalStateException if an order is in incorrect state
     */
    @Override
    public void prepareOrder(UUID orderId) {
        validateOrderExists(orderId);
        executeOrderAction(orderId, Order::prepare, "Preparing");
    }

    /*
     * move order to status deliver, remove it from activeOrders put it to finished
     * @throws IllegalArgumentException if an order does not exist
     * @throws IllegalStateException if an order is in incorrect state
     */
    @Override
    public OrderDTO deliverOrder(UUID orderId) {
        Order order = validateOrderExists(orderId);
        synchronized (order) {
            order.deliver();
            logger.logOrderDelivered(order);

            List<Pancake> pancakes = orderPancakes.remove(orderId);
            activeOrders.remove(orderId);
            finishedOrders.put(orderId, order);

            List<PancakeDTO> pancakeDTOList =
                    pancakes.stream()
                            .map(p -> new PancakeDTO(
                                    p.getOrderId(),
                                    p.getPancakeId(),
                                    p.getIngredients().stream().map(Ingredient::displayName).toList(),
                                    p.getDescription()))
                            .toList();

            return new OrderDTO(
                    order.getId(),
                    order.getBuilding(),
                    order.getRoom(),
                    order.getStatus().name(),
                    pancakeDTOList);
        }
    }

    /*
     * move order to status canceled
     * @throws IllegalArgumentException if an order does not exist
     * @throws IllegalStateException if an order is in incorrect state
     */
    @Override
    public void cancelOrder(UUID orderId) {
        Order order = validateOrderExists(orderId);
        executeOrderAction(orderId, Order::cancel, "Cancelled");
        activeOrders.remove(orderId);
        orderPancakes.remove(orderId);
        finishedOrders.put(orderId, order);
    }

    /*
     * clear finished orders( finished orders is just map with orders that has beem cancelled or delivered)
     */
    @Override
    public void clearAllFinishedOrders() {
        finishedOrders.clear();
    }

    /*
     * retrieve order status
     * @throws IllegalArgumentException if an order does not exist
     */
    @Override
    public OrderDTO getOrderStatus(UUID orderId) {
        Order order = validateOrderExists(orderId);
        List<PancakeDTO> pancakeDTOs = orderPancakes.getOrDefault(orderId, List.of()).stream()
                .map(p -> new PancakeDTO(
                        p.getOrderId(),
                        p.getPancakeId(),
                        p.getIngredients().stream().map(Ingredient::displayName).toList(),
                        p.getDescription()))
                .toList();
        return new OrderDTO(
                order.getId(),
                order.getBuilding(),
                order.getRoom(),
                order.getStatus().name(),
                pancakeDTOs);
    }

    /*
     * get pancake description
     */
    @Override
    public List<PancakeDTO> getPancakeDescriptions(UUID orderId) {
        return orderPancakes.getOrDefault(orderId, List.of()).stream()
                .map(p -> new PancakeDTO(
                        p.getOrderId(),
                        p.getPancakeId(),
                        p.getIngredients().stream().map(Ingredient::displayName).toList(),
                        p.getDescription()))
                .toList();
    }

    /*
     * view order
     */
    @Override
    public List<String> viewOrder(UUID orderId) {
        return orderPancakes.getOrDefault(orderId, List.of()).stream()
                .map(Pancake::getDescription)
                .toList();
    }


    /*
     * find all orders with given status
     */
    @Override
    public Set<UUID> listOrdersWithStatus(OrderStatus orderStatus) {
        return Stream.concat(
                        activeOrders.entrySet().stream()
                                .filter(e -> e.getValue().getStatus().equals(orderStatus)),
                        finishedOrders.entrySet().stream()
                                .filter(e -> e.getValue().getStatus().equals(orderStatus)))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }


    // ================== Internal Methods ================== //


    /*
     * validate Order Exists
     * @throws IllegalArgumentException if an order does not exist
     */
    private Order validateOrderExists(UUID orderId) {
        Order order = activeOrders.get(orderId);
        if (order == null) {
            order = finishedOrders.get(orderId);
            if (order == null) {
                throw new IllegalArgumentException("Order " + orderId + " not found");
            }
        }
        return order;
    }

    /*
     * validate Order state
     * @throws IllegalArgumentException if an order does not exist
     */
    private void validateOrderState(UUID orderId, OrderStatus expected) {
        Order order = validateOrderExists(orderId);
        if (!order.getStatus().equals(expected)) {
            throw new IllegalStateException(
                    String.format("Order %s must be %s (current: %s)",
                            orderId, expected, order.getStatus()));
        }
    }


    /*
     * executes axcion
     * @throws IllegalArgumentException if an order does not exist
     */
    private void executeOrderAction(UUID orderId, Consumer<Order> action, String actionName) {
        Order order = validateOrderExists(orderId);
        synchronized (order) {
            try {
                action.accept(order);
                logger.logOrderStatusChange(order, actionName);
            } catch (IllegalStateException e) {
                logger.logInvalidTransition(order, actionName);
                throw e;
            }
        }
    }

    /*
     * internal action of addingPancakes
     * @throws IllegalArgumentException if quantity is incorrect
     * @throws IllegaleStateException if state is incorrect
     */
    private void addPancakesInternal(UUID orderId, List<Ingredient> ingredients, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        validateOrderState(orderId, OrderStatus.NEW);

        orderPancakes.computeIfPresent(orderId, (id, pancakes) -> {
            for (int i = 0; i < quantity; i++) {
                Pancake pancake = PancakeFactory.createPancake(orderId, ingredients);
                pancakes.add(pancake);
                logger.logPancakeAdded(orderId, pancake);
            }
            return pancakes;
        });
    }

}