package com.shop.order.domain;

import com.shop.common.exception.BusinessException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Named CustomerOrder because "Order" collides with the JPQL/SQL keyword ORDER. Table is still "orders". */
@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor
public class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version                      // optimistic locking: concurrent updates -> ObjectOptimisticLockingFailureException -> 409
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate  void onUpdate() { updatedAt = Instant.now(); }

    public void addItem(OrderItem item) { item.setOrder(this); items.add(item); }

    // Rich domain behaviour: state rules live in the entity, not scattered in services
    public void confirm() { status = OrderStatus.CONFIRMED; }

    public void cancel() {
        if (status == OrderStatus.CONFIRMED) {
            throw new BusinessException("ORDER_ALREADY_CONFIRMED", "Confirmed orders cannot be cancelled");
        }
        status = OrderStatus.CANCELLED;
    }
}
