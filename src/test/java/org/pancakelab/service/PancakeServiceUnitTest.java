package org.pancakelab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pancakelab.dto.OrderDTO;
import org.pancakelab.dto.PancakeDTO;
import org.pancakelab.model.enums.OrderStatus;
import org.pancakelab.service.interfaces.PancakeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.pancakelab.service.PancakeServiceTest.DARK_CHOCOLATE_INGREDIENT;
import static org.pancakelab.service.PancakeServiceTest.MILK_CHOCOLATE_INGREDIENT;

public class PancakeServiceUnitTest {
    private final PancakeService pancakeService= new PancakeServiceImpl(OrderLogger.getInstance());

    @BeforeEach
    public void setUp() {
        // Clear orders from activeOrders and orderPancakes
        Set<UUID> newOrders = pancakeService.listOrdersWithStatus(OrderStatus.NEW);
        Set<UUID> completedOrders = pancakeService.listOrdersWithStatus(OrderStatus.COMPLETED);
        Set<UUID> preparingOrders = pancakeService.listOrdersWithStatus(OrderStatus.PREPARING);

        // Cancel orders in NEW or COMPLETED status to remove them from activeOrders and orderPancakes
        newOrders.forEach(pancakeService::cancelOrder);
        completedOrders.forEach(pancakeService::cancelOrder);

        // Deliver orders in PREPARING status to remove them from activeOrders and orderPancakes
        preparingOrders.forEach(pancakeService::deliverOrder);

        // Clear completedOrders (which now contains all orders after cancelling or delivering)
        pancakeService.clearAllFinishedOrders();

        // Clear logs
        OrderLogger.getInstance().clearLogs();
    }

    @Test
    public void testCreateOrder_initialStateIsCorrect() {
        OrderDTO order = pancakeService.createOrder(5, 10);

        assertNotNull(order.id());
        assertEquals(5, order.building());
        assertEquals(10, order.room());
        assertEquals(OrderStatus.NEW.name(), order.status());
        assertTrue(order.pancakes().isEmpty());

        Set<UUID> newOrders = pancakeService.listOrdersWithStatus(OrderStatus.NEW);
        assertEquals(Set.of(order.id()), newOrders);
    }

    @Test
    public void testAddPancake_pancakeIsAddedSuccessfully() {
        OrderDTO order = pancakeService.createOrder(3, 15);
        pancakeService.addPancakes(order.id(),List.of(DARK_CHOCOLATE_INGREDIENT), 1);

        List<String> pancakes = pancakeService.viewOrder(order.id());
        assertEquals(List.of("Delicious pancake with dark chocolate!"), pancakes);

        OrderDTO orderStatus = pancakeService.getOrderStatus(order.id());
        assertEquals(1, orderStatus.pancakes().size());
        assertEquals("Delicious pancake with dark chocolate!", orderStatus.pancakes().get(0).description());
    }

    @Test
    public void testCompleteOrder_statusIsUpdated() {
        OrderDTO order = pancakeService.createOrder(2, 20);
        pancakeService.addPancakes(order.id(),List.of(DARK_CHOCOLATE_INGREDIENT), 1);
        pancakeService.completeOrder(order.id());

        Set<UUID> completedOrders = pancakeService.listOrdersWithStatus(OrderStatus.COMPLETED);
        assertEquals(Set.of(order.id()), completedOrders);

        OrderDTO orderStatus = pancakeService.getOrderStatus(order.id());
        assertEquals(OrderStatus.COMPLETED.name(), orderStatus.status());
    }

    @Test
    public void testCancelOrder_orderIsRemovedFromActiveOrders() {
        OrderDTO order = pancakeService.createOrder(1, 5);
        pancakeService.cancelOrder(order.id());

        Set<UUID> newOrders = pancakeService.listOrdersWithStatus(OrderStatus.NEW);
        assertFalse(newOrders.contains(order.id()));

        Set<UUID> cancelledOrders = pancakeService.listOrdersWithStatus(OrderStatus.CANCELLED);
        assertEquals(Set.of(order.id()), cancelledOrders);

        List<String> pancakes = pancakeService.viewOrder(order.id());
        assertTrue(pancakes.isEmpty());
    }

    @Test
    public void testAddPancakeWithInvalidQuantity_throwsException() {
        OrderDTO order = pancakeService.createOrder(4, 8);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pancakeService.addPancakes(order.id(),List.of(MILK_CHOCOLATE_INGREDIENT), 0);
        });
        assertEquals("Quantity must be positive", exception.getMessage());

        List<String> pancakes = pancakeService.viewOrder(order.id());
        assertTrue(pancakes.isEmpty());
    }

    @Test
    void testConcurrentOrderCreation() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<OrderDTO>> futures = new ArrayList<>();

        // Submit multiple createOrder tasks
        for (int i = 0; i < threadCount; i++) {
            final int building = i + 1;
            final int room = i + 10;
            futures.add(executor.submit(() -> pancakeService.createOrder(building, room)));
        }

        // Collect results
        List<OrderDTO> orders = new ArrayList<>();
        for (Future<OrderDTO> future : futures) {
            orders.add(future.get());
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Assertions
        assertEquals(threadCount, orders.size(), "All orders should be created successfully");
        for (int i = 0; i < threadCount; i++) {
            OrderDTO order = orders.get(i);
            assertNotNull(order.id(), "Order ID should not be null");
            assertEquals(i + 1, order.building(), "Building number should match");
            assertEquals(i + 10, order.room(), "Room number should match");
            assertEquals(OrderStatus.NEW.name(), order.status(), "Status should be NEW");
            assertTrue(order.pancakes().isEmpty(), "No pancakes should be present initially");
        }

        // Check for unique order IDs
        long uniqueIds = orders.stream().map(OrderDTO::id).distinct().count();
        assertEquals(threadCount, uniqueIds, "All order IDs should be unique");
    }

    @Test
    void testConcurrentAddPancakes() throws InterruptedException, ExecutionException {
        // Create an order first
        OrderDTO order = pancakeService.createOrder(1, 1);
        UUID orderId = order.id();
        List<String> ingredients = List.of(DARK_CHOCOLATE_INGREDIENT);
        int quantity = 5;
        int threadCount = 5;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        // Submit multiple addPancakes tasks
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> pancakeService.addPancakes(orderId, ingredients, quantity)));
        }

        // Collect results
        for (Future<?> future : futures) {
            future.get(); // Wait for completion, will throw if exception occurs
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Assertions
        OrderDTO updatedOrder = pancakeService.getOrderStatus(orderId);
        int expectedPancakes = quantity * threadCount; // 5 threads * 5 pancakes each
        assertEquals(expectedPancakes, updatedOrder.pancakes().size(),
                "Total number of pancakes should match expected count");
        updatedOrder.pancakes().forEach(p -> {
            assertEquals("dark chocolate", p.ingredients().get(0),
                    "All pancakes should have dark chocolate");
        });
    }

    @Test
    void testConcurrentRemovePancake() throws InterruptedException, ExecutionException {
        // Create an order and add pancakes
        OrderDTO order = pancakeService.createOrder(1, 1);
        UUID orderId = order.id();
        List<String> ingredients = List.of(DARK_CHOCOLATE_INGREDIENT);
        pancakeService.addPancakes(orderId, ingredients, 3); // Add 3 pancakes

        // Get initial pancake IDs
        List<PancakeDTO> initialPancakes = pancakeService.getPancakeDescriptions(orderId);
        assertEquals(3, initialPancakes.size(), "Initial pancake count should be 3");

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        // Submit multiple removePancake tasks
        for (int i = 0; i < threadCount; i++) {
            int index = i % initialPancakes.size(); // Ensure we target existing pancakes
            UUID pancakeId = initialPancakes.get(index).pancakeId();
            futures.add(executor.submit(() -> pancakeService.removePancake(orderId, pancakeId)));
        }

        // Collect results
        for (Future<?> future : futures) {
            future.get(); // Wait for completion, will throw if exception occurs
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Assertions
        OrderDTO updatedOrder = pancakeService.getOrderStatus(orderId);
        int expectedRemaining = initialPancakes.size() - threadCount; // 3 - 3 = 0 if all removed
        assertEquals(Math.max(0, expectedRemaining), updatedOrder.pancakes().size(),
                "Remaining pancakes should reflect removals (at least 0)");
    }
}
