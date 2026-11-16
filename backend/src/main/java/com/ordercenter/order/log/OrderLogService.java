package com.ordercenter.order.log;

import com.ordercenter.auth.AuthPrincipal;
import com.ordercenter.order.Order;
import com.ordercenter.order.log.dto.OrderLogResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderLogService {

    private static final String SYSTEM_OPERATOR = "系统";

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
        log.setOperator(currentOperator());
        orderOperationLogRepository.save(log);
    }

    private static String currentOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthPrincipal principal) {
            return principal.displayName();
        }
        return SYSTEM_OPERATOR;
    }

    public List<OrderLogResponse> list(Long orderId) {
        return orderOperationLogRepository
                .findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(OrderLogResponse::from)
                .toList();
    }
}
