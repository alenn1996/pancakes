package org.pancakelab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pancakelab.dto.OrderDTO;
import org.pancakelab.dto.PancakeDTO;
import org.pancakelab.model.enums.OrderStatus;
import org.pancakelab.service.interfaces.PancakeService;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.pancakelab.service.PancakeServiceTest.DARK_CHOCOLATE_INGREDIENT;
import static org.pancakelab.service.PancakeServiceTest.MILK_CHOCOLATE_INGREDIENT;

public class PancakeServiceUnitTest {
    private final PancakeService pancakeService = new PancakeServiceImpl(OrderLogger.getInstance());

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
        pancakeService.addPancakes(order.id(), List.of(DARK_CHOCOLATE_INGREDIENT), 1);

        List<String> pancakes = pancakeService.viewOrder(order.id());
        assertEquals(List.of("Delicious pancake with dark chocolate!"), pancakes);

        OrderDTO orderStatus = pancakeService.getOrderStatus(order.id());
        assertEquals(1, orderStatus.pancakes().size());
        assertEquals("Delicious pancake with dark chocolate!", orderStatus.pancakes().get(0).description());
    }

    @Test
    public void testCompleteOrder_statusIsUpdated() {
        OrderDTO order = pancakeService.createOrder(2, 20);
        pancakeService.addPancakes(order.id(), List.of(DARK_CHOCOLATE_INGREDIENT), 1);
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

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                pancakeService.addPancakes(order.id(), List.of(MILK_CHOCOLATE_INGREDIENT), 0)
        );
        assertEquals("Quantity must be positive", exception.getMessage());

        List<String> pancakes = pancakeService.viewOrder(order.id());
        assertTrue(pancakes.isEmpty());
    }

    @Test
    void testConcurrentOrderCreation() throws InterruptedException, ExecutionException {
        int threadCount = 10;

        try (AutoCloseableExecutorService acExecutor = new AutoCloseableExecutorService(
                Executors.newFixedThreadPool(threadCount))) {
            ExecutorService executor = acExecutor.get();
            List<Callable<OrderDTO>> tasks = new ArrayList<>();
            Set<Integer> expectedBuildings = new HashSet<>();
            Set<Integer> expectedRooms = new HashSet<>();

            for (int i = 0; i < threadCount; i++) {
                final int building = i + 1;
                final int room = i + 10;
                expectedBuildings.add(building);
                expectedRooms.add(room);
                tasks.add(() -> pancakeService.createOrder(building, room));
            }

            List<Future<OrderDTO>> futures = executor.invokeAll(tasks);
            List<OrderDTO> orders = new ArrayList<>();
            for (Future<OrderDTO> future : futures) {
                orders.add(future.get());
            }

            // Assertions
            assertEquals(threadCount, orders.size(), "All orders should be created successfully");

            Set<UUID> ids = new HashSet<>();
            Set<Integer> actualBuildings = new HashSet<>();
            Set<Integer> actualRooms = new HashSet<>();

            for (OrderDTO order : orders) {
                assertNotNull(order.id(), "Order ID should not be null");
                assertEquals(OrderStatus.NEW.name(), order.status(), "Status should be NEW");
                assertTrue(order.pancakes().isEmpty(), "No pancakes should be present initially");
                ids.add(order.id());
                actualBuildings.add(order.building());
                actualRooms.add(order.room());
            }

            assertEquals(threadCount, ids.size(), "All order IDs should be unique");
            assertEquals(expectedBuildings, actualBuildings, "All expected building numbers should be used");
            assertEquals(expectedRooms, actualRooms, "All expected room numbers should be used");
        }
    }

    @Test
    void testConcurrentAddPancakes() throws InterruptedException, ExecutionException {
        // Create an order first
        OrderDTO order = pancakeService.createOrder(1, 1);
        UUID orderId = order.id();
        List<String> ingredients = List.of(DARK_CHOCOLATE_INGREDIENT);
        int quantity = 5;
        int threadCount = 5;

        try (AutoCloseableExecutorService acExecutor =
                     new AutoCloseableExecutorService(Executors.newFixedThreadPool(threadCount))) {
            ExecutorService executor = acExecutor.get();
            List<Future<?>> futures = new ArrayList<>();

            // Submit multiple addPancakes tasks
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> pancakeService.addPancakes(orderId, ingredients, quantity)));
            }

            // Wait for all tasks to complete
            for (Future<?> future : futures) {
                future.get();
            }
        }

        // Assertions
        OrderDTO updatedOrder = pancakeService.getOrderStatus(orderId);
        int expectedPancakes = quantity * threadCount;
        assertEquals(expectedPancakes, updatedOrder.pancakes().size(),
                "Total number of pancakes should match expected count");
        updatedOrder.pancakes().forEach(p ->
                assertEquals("dark chocolate", p.ingredients().get(0),
                        "All pancakes should have dark chocolate")
        );
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

        try (AutoCloseableExecutorService acExecutor =
                     new AutoCloseableExecutorService(Executors.newFixedThreadPool(threadCount))) {
            ExecutorService executor = acExecutor.get();
            List<Future<?>> futures = new ArrayList<>();

            // Submit multiple removePancake tasks
            for (int i = 0; i < threadCount; i++) {
                int index = i % initialPancakes.size(); // Ensure we target existing pancakes
                UUID pancakeId = initialPancakes.get(index).pancakeId();
                futures.add(executor.submit(() -> pancakeService.removePancake(orderId, pancakeId)));
            }

            // Wait for all tasks to complete
            for (Future<?> future : futures) {
                future.get(); // Will throw if exception occurs in task
            }
        }

        // Assertions
        OrderDTO updatedOrder = pancakeService.getOrderStatus(orderId);
        int expectedRemaining = initialPancakes.size() - threadCount; // 3 - 3 = 0 if all removed
        assertEquals(Math.max(0, expectedRemaining), updatedOrder.pancakes().size(),
                "Remaining pancakes should reflect removals (at least 0)");
    }
}
