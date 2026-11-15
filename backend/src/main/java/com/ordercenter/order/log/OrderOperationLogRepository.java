package com.ordercenter.order.log;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderOperationLogRepository extends JpaRepository<OrderOperationLog, Long> {

    List<OrderOperationLog> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
