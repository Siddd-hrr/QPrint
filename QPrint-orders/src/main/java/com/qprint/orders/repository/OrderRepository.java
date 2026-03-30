package com.qprint.orders.repository;

import com.qprint.orders.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Order> findByUserIdAndStatusNotInOrderByCreatedAtDesc(UUID userId, List<com.qprint.orders.model.OrderStatus> statuses);

    Order findFirstByCheckoutId(String checkoutId);
}
