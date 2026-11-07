package com.shop.order.repository;

import com.shop.order.domain.CustomerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    // One JOIN query instead of 1 + N lazy loads
    @EntityGraph(attributePaths = "items")
    Optional<CustomerOrder> findWithItemsById(UUID id);

    // Served by ix_orders_user_created (user_id, created_at DESC); list view does not touch items
    Page<CustomerOrder> findByUserId(UUID userId, Pageable pageable);
}
