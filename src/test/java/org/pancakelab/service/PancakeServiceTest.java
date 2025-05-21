package org.pancakelab.service;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.pancakelab.dto.OrderDTO;
import org.pancakelab.dto.PancakeDTO;
import org.pancakelab.model.enums.OrderStatus;
import org.pancakelab.service.interfaces.PancakeService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PancakeServiceTest {
    private final PancakeService pancakeService= new PancakeServiceImpl(OrderLogger.getInstance());
    private OrderDTO order = null;

    public final static String DARK_CHOCOLATE_INGREDIENT="dark chocolate";
    public final static String MILK_CHOCOLATE_INGREDIENT="milk chocolate";
    public final static String WHIPPED_CREAM_INGREDIENT="whipped cream";
    public final static String HAZELNUTS_INGREDIENT="hazelnuts";
    private final static String INITIAL_PANCAKE_DESCRIPTION="Delicious pancake with ";
    private final static String DARK_CHOCOLATE_PANCAKE_DESCRIPTION = INITIAL_PANCAKE_DESCRIPTION +  DARK_CHOCOLATE_INGREDIENT + "!";
    private final static String MILK_CHOCOLATE_PANCAKE_DESCRIPTION = INITIAL_PANCAKE_DESCRIPTION + MILK_CHOCOLATE_INGREDIENT + "!";
    private final static String MILK_CHOCOLATE_HAZELNUTS_PANCAKE_DESCRIPTION = INITIAL_PANCAKE_DESCRIPTION + MILK_CHOCOLATE_INGREDIENT + ", " + HAZELNUTS_INGREDIENT + "!";


    @Test
    @org.junit.jupiter.api.Order(10)
    public void GivenOrderDoesNotExist_WhenCreatingOrder_ThenOrderCreatedWithCorrectData_Test() {
        order = pancakeService.createOrder(10, 20);
        assertEquals(10, order.building());
        assertEquals(20, order.room());
        assertEquals(OrderStatus.NEW.name(), order.status());
        assertTrue(order.pancakes().isEmpty());
    }

    @Test
    @org.junit.jupiter.api.Order(20)
    public void GivenOrderExists_WhenAddingPancakes_ThenCorrectNumberOfPancakesAdded_Test() {
        addPancakes();
        List<String> ordersPancakes = pancakeService.viewOrder(order.id());
        assertEquals(List.of(
                DARK_CHOCOLATE_PANCAKE_DESCRIPTION,
                DARK_CHOCOLATE_PANCAKE_DESCRIPTION,
                DARK_CHOCOLATE_PANCAKE_DESCRIPTION,
                MILK_CHOCOLATE_PANCAKE_DESCRIPTION,
                MILK_CHOCOLATE_PANCAKE_DESCRIPTION,
                MILK_CHOCOLATE_PANCAKE_DESCRIPTION,
                MILK_CHOCOLATE_HAZELNUTS_PANCAKE_DESCRIPTION,
                MILK_CHOCOLATE_HAZELNUTS_PANCAKE_DESCRIPTION,
                MILK_CHOCOLATE_HAZELNUTS_PANCAKE_DESCRIPTION), ordersPancakes);
    }

    @Test
    @org.junit.jupiter.api.Order(30)
    public void GivenPancakesExists_WhenRemovingPancakes_ThenCorrectNumberOfPancakesRemoved_Test() {
        pancakeService.removePancakes(DARK_CHOCOLATE_PANCAKE_DESCRIPTION, order.id(), 2);
        pancakeService.removePancakes(MILK_CHOCOLATE_PANCAKE_DESCRIPTION, order.id(), 3);
        pancakeService.removePancakes(MILK_CHOCOLATE_HAZELNUTS_PANCAKE_DESCRIPTION, order.id(), 1);
        List<String> ordersPancakes = pancakeService.viewOrder(order.id());
        assertEquals(List.of(
                DARK_CHOCOLATE_PANCAKE_DESCRIPTION,
                MILK_CHOCOLATE_HAZELNUTS_PANCAKE_DESCRIPTION,
                MILK_CHOCOLATE_HAZELNUTS_PANCAKE_DESCRIPTION), ordersPancakes);
    }

    @Test
    @org.junit.jupiter.api.Order(40)
    public void GivenOrderExists_WhenCompletingOrder_ThenOrderCompleted_Test() {
        pancakeService.completeOrder(order.id());
        Set<UUID> completedOrders = pancakeService.listOrdersWithStatus(OrderStatus.COMPLETED);
        assertTrue(completedOrders.contains(order.id()));
    }

    @Test
    @org.junit.jupiter.api.Order(50)
    public void GivenOrderExists_WhenPreparingOrder_ThenOrderPrepared_Test() {
        pancakeService.prepareOrder(order.id());
        Set<UUID> completedOrders = pancakeService.listOrdersWithStatus(OrderStatus.COMPLETED);
        assertFalse(completedOrders.contains(order.id()));
        Set<UUID> preparedOrders = pancakeService.listOrdersWithStatus(OrderStatus.PREPARING);
        assertTrue(preparedOrders.contains(order.id()));
    }

    @Test
    @org.junit.jupiter.api.Order(60)
    public void GivenOrderExists_WhenDeliveringOrder_ThenCorrectOrderReturnedAndOrderRemovedFromTheDatabase_Test() {
        List<String> pancakesToDeliver = pancakeService.viewOrder(order.id());
        OrderDTO deliveredOrder = pancakeService.deliverOrder(order.id());
        Set<UUID> completedOrders = pancakeService.listOrdersWithStatus(OrderStatus.COMPLETED);
        assertFalse(completedOrders.contains(order.id()));
        Set<UUID> preparedOrders = pancakeService.listOrdersWithStatus(OrderStatus.PREPARING);
        assertFalse(preparedOrders.contains(order.id()));
        List<String> ordersPancakes = pancakeService.viewOrder(order.id());
        assertEquals(List.of(), ordersPancakes);
        assertEquals(order.id(), deliveredOrder.id());
        assertEquals(pancakesToDeliver, deliveredOrder.pancakes().stream().map(PancakeDTO::description).toList());
        order = null;
    }

    @Test
    @org.junit.jupiter.api.Order(70)
    public void GivenOrderExists_WhenCancellingOrder_ThenOrderAndPancakesRemoved_Test() {
        order = pancakeService.createOrder(10, 20);
        addPancakes();
        pancakeService.cancelOrder(order.id());
        Set<UUID> completedOrders = pancakeService.listOrdersWithStatus(OrderStatus.COMPLETED);
        assertFalse(completedOrders.contains(order.id()));
        Set<UUID> preparedOrders = pancakeService.listOrdersWithStatus(OrderStatus.PREPARING);
        assertFalse(preparedOrders.contains(order.id()));
        List<String> ordersPancakes = pancakeService.viewOrder(order.id());
        assertEquals(List.of(), ordersPancakes);
    }

    @Test
    @org.junit.jupiter.api.Order(80)
    public void GivenOrderExists_WhenAddingCustomPancake_ThenPancakeAdded_Test() {
        order = pancakeService.createOrder(5, 15);
        pancakeService.addPancakes(order.id(), List.of("dark chocolate", "hazelnuts"), 1);
        List<String> ordersPancakes = pancakeService.viewOrder(order.id());
        assertEquals(List.of("Delicious pancake with dark chocolate, hazelnuts!"), ordersPancakes);
    }
    @Test
    @org.junit.jupiter.api.Order(90)
    public void GivenPancakesExist_WhenRemovingPancakeByIngredients_ThenPancakeRemoved_Test() {
        pancakeService.addPancakes(order.id(), List.of("dark chocolate", "hazelnuts"),1);
        List<String> initialPancakes = pancakeService.viewOrder(order.id());
        assertEquals(2, initialPancakes.size());
        pancakeService.removePancake(order.id(), List.of("dark chocolate", "hazelnuts"));
        List<String> remainingPancakes = pancakeService.viewOrder(order.id());
        assertEquals(1, remainingPancakes.size());
        assertEquals("Delicious pancake with dark chocolate, hazelnuts!", remainingPancakes.get(0));
    }

    @Test
    @org.junit.jupiter.api.Order(100)
    public void GivenPancakesExist_WhenRemovingPancakeById_ThenPancakeRemoved_Test() {
        pancakeService.addPancakes(order.id(), List.of("milk chocolate"),1);
        List<PancakeDTO> pancakes = pancakeService.getPancakeDescriptions(order.id());
        assertEquals(2, pancakes.size());
        UUID pancakeId = pancakes.get(1).pancakeId(); // Second pancake (milk chocolate)
        pancakeService.removePancake(order.id(), pancakeId);
        List<String> remainingPancakes = pancakeService.viewOrder(order.id());
        assertEquals(1, remainingPancakes.size());
        assertEquals("Delicious pancake with dark chocolate, hazelnuts!", remainingPancakes.get(0));
    }

    @Test
    @org.junit.jupiter.api.Order(110)
    public void GivenCompletedOrdersExist_WhenClearingCompletedOrders_ThenCompletedOrdersCleared_Test() {

        pancakeService.cancelOrder(order.id());
        assertFalse(pancakeService.listOrdersWithStatus(OrderStatus.CANCELLED).isEmpty());
        pancakeService.clearAllFinishedOrders();
        assertTrue(pancakeService.listOrdersWithStatus(OrderStatus.CANCELLED).isEmpty());
    }

    @Test
    @org.junit.jupiter.api.Order(120)
    public void GivenOrderExists_WhenGettingOrderStatus_ThenCorrectStatusReturned_Test() {
        order = pancakeService.createOrder(7, 25);
        pancakeService.addPancakes(order.id(),List.of(DARK_CHOCOLATE_INGREDIENT), 1);
        OrderDTO orderStatus = pancakeService.getOrderStatus(order.id());
        assertEquals(order.id(), orderStatus.id());
        assertEquals(order.building(), orderStatus.building());
        assertEquals(order.room(), orderStatus.room());
        assertEquals(OrderStatus.NEW.name(), orderStatus.status());
        assertEquals(1, orderStatus.pancakes().size());
        assertEquals(DARK_CHOCOLATE_PANCAKE_DESCRIPTION, orderStatus.pancakes().get(0).description());
    }

    @Test
    @org.junit.jupiter.api.Order(130)
    public void GivenOrderExists_WhenGettingPancakeDescriptions_ThenCorrectDescriptionsReturned_Test() {
        pancakeService.addPancakes(order.id(),List.of(MILK_CHOCOLATE_INGREDIENT), 1);
        List<PancakeDTO> descriptions = pancakeService.getPancakeDescriptions(order.id());
        assertEquals(2, descriptions.size());
        assertTrue(descriptions.stream().anyMatch(dto -> dto.description().equals(DARK_CHOCOLATE_PANCAKE_DESCRIPTION)));
        assertTrue(descriptions.stream().anyMatch(dto -> dto.description().equals(MILK_CHOCOLATE_PANCAKE_DESCRIPTION)));
    }

    @Test
    @org.junit.jupiter.api.Order(140)
    public void GivenInvalidState_WhenPreparingOrder_ThenExceptionThrown_Test() {
        order = pancakeService.createOrder(2, 8);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            pancakeService.prepareOrder(order.id());
        });
        assertEquals(String.format("Order must be %s (current: %s)", OrderStatus.COMPLETED, OrderStatus.NEW), exception.getMessage());
    }

    @Test
    @org.junit.jupiter.api.Order(150)
    public void GivenNonExistentOrder_WhenGettingOrderStatus_ThenExceptionThrown_Test() {
        UUID nonExistentOrderId = UUID.randomUUID();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pancakeService.getOrderStatus(nonExistentOrderId);
        });
        assertEquals("Order " + nonExistentOrderId + " not found", exception.getMessage());
    }

    @Test
    @org.junit.jupiter.api.Order(160)
    public void GivenInvalidQuantity_WhenAddingPancakes_ThenExceptionThrown_Test() {
        order = pancakeService.createOrder(1, 1);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pancakeService.addPancakes(order.id(),List.of(DARK_CHOCOLATE_INGREDIENT),0 );
        });
        assertEquals("Quantity must be positive", exception.getMessage());
    }


    private void addPancakes() {
        pancakeService.addPancakes(order.id(),List.of(DARK_CHOCOLATE_INGREDIENT), 3);
        pancakeService.addPancakes(order.id(),List.of(MILK_CHOCOLATE_INGREDIENT), 3);
        pancakeService.addPancakes(order.id(),List.of(MILK_CHOCOLATE_INGREDIENT, HAZELNUTS_INGREDIENT), 3);
    }
}
