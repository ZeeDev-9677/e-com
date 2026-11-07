package com.shop.order.repository;

import com.shop.order.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /** SKIP LOCKED: several pods can poll concurrently without double-sending or blocking each other. */
    @Query(value = "select * from outbox_event where published = false order by created_at limit :n for update skip locked",
           nativeQuery = true)
    List<OutboxEvent> lockBatch(@Param("n") int n);

    @Modifying
    @Query("delete from OutboxEvent e where e.published = true and e.createdAt < :before")
    int deletePublishedBefore(@Param("before") Instant before);
}
