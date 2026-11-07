package com.shop.order.messaging;

import com.shop.order.repository.OrderReportDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Safety net: PENDING orders older than 30 min are cancelled by a stored procedure. Idempotent, so running
 *  on every replica is harmless; use ShedLock if the job were not idempotent. */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaleOrderJob {
    private final OrderReportDao dao;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void run() {
        dao.cancelStale(30);
        log.info("stale_orders_sweep_done");
    }
}
