package com.ordercenter.order;

import com.ordercenter.common.PageResult;
import com.ordercenter.order.dto.CreateOrderRequest;
import com.ordercenter.order.dto.OrderResponse;
import com.ordercenter.shipping.ShippingMethodCatalog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final int MAX_PAGE_SIZE = 200;

    private final OrderRepository orderRepository;
    private final ShippingMethodCatalog shippingMethodCatalog;

    public OrderService(OrderRepository orderRepository, ShippingMethodCatalog shippingMethodCatalog) {
        this.orderRepository = orderRepository;
        this.shippingMethodCatalog = shippingMethodCatalog;
    }

    public PageResult<OrderResponse> page(int page, int size, String keyword, OrderStatus status) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String safeKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;

        Specification<Order> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (safeKeyword != null) {
                String like = "%" + safeKeyword + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderNo")), like),
                        cb.like(cb.lower(root.get("customerName")), like),
                        cb.like(cb.lower(root.get("productName")), like)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<Order> result = orderRepository.findAll(
                specification,
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<OrderResponse> records = result.getContent().stream()
                .map(order -> OrderResponse.from(
                        order,
                        shippingMethodCatalog.labelOf(order.getShippingMethod())
                ))
                .toList();

        return new PageResult<>(records, result.getTotalElements(), safePage, safeSize);
    }

    /**
     * 新增订单。
     * 注意：类上是 readOnly=true，所以写操作必须显式覆盖为读写事务。
     */
    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        shippingMethodCatalog.validate(request.shippingMethod(), request.recipientCountry());

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setShippingMethod(request.shippingMethod().trim().toUpperCase(Locale.ROOT));
        order.setRecipientCountry(request.recipientCountry().trim().toUpperCase(Locale.ROOT));
        order.setCustomerName(request.customerName().trim());
        order.setCustomerPhone(request.customerPhone().trim());
        order.setProductName(request.productName().trim());
        order.setQuantity(request.quantity());
        order.setUnitPrice(request.unitPrice());
        order.setTotalAmount(
                request.unitPrice()
                        .multiply(BigDecimal.valueOf(request.quantity()))
                        .setScale(2, RoundingMode.HALF_UP)
        );
        order.setStatus(OrderStatus.FORECAST);
        order.setRemark(StringUtils.hasText(request.remark()) ? request.remark().trim() : null);

        Order saved = orderRepository.save(order);
        return OrderResponse.from(saved, shippingMethodCatalog.labelOf(saved.getShippingMethod()));
    }

    private String generateOrderNo() {
        String timePart = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "ORD" + timePart + randomPart;
    }
}
