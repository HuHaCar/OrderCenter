package com.ordercenter.order.log;

import com.ordercenter.order.Order;
import com.ordercenter.order.log.dto.OrderLogResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderLogService {

    private static final String DEFAULT_OPERATOR = "管理员";

    private final OrderOperationLogRepository orderOperationLogRepository;

    public OrderLogService(OrderOperationLogRepository orderOperationLogRepository) {
        this.orderOperationLogRepository = orderOperationLogRepository;
    }

    @Transactional
    public void record(Order order, OrderLogAction action, String description) {
        OrderOperationLog log = new OrderOperationLog();
        log.setOrderId(order.getId());
        log.setOrderNo(order.getOrderNo());
        log.setAction(action.name());
        log.setDescription(description);
        log.setOperator(DEFAULT_OPERATOR);
        orderOperationLogRepository.save(log);
    }

    public List<OrderLogResponse> list(Long orderId) {
        return orderOperationLogRepository
                .findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(OrderLogResponse::from)
                .toList();
    }
}
