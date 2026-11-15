package com.ordercenter.order;

import com.ordercenter.common.PageResult;
import com.ordercenter.order.dto.CreateDraftOrderRequest;
import com.ordercenter.order.dto.CreateOrderRequest;
import com.ordercenter.order.dto.OrderResponse;
import com.ordercenter.order.dto.UpdateOrderRequest;
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

    /**
     * 保存草稿：只校验运输方式和收件国家，其他字段允许为空。
     */
    @Transactional
    public OrderResponse createDraft(CreateDraftOrderRequest request) {
        shippingMethodCatalog.validate(request.shippingMethod(), request.recipientCountry());

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setShippingMethod(request.shippingMethod().trim().toUpperCase(Locale.ROOT));
        order.setRecipientCountry(request.recipientCountry().trim().toUpperCase(Locale.ROOT));
        order.setCustomerName(trimToNull(request.customerName()));
        order.setCustomerPhone(trimToNull(request.customerPhone()));
        order.setProductName(trimToNull(request.productName()));
        order.setQuantity(request.quantity());
        order.setUnitPrice(request.unitPrice());
        order.setRemark(trimToNull(request.remark()));
        if (request.quantity() != null && request.unitPrice() != null) {
            order.setTotalAmount(
                    request.unitPrice()
                            .multiply(BigDecimal.valueOf(request.quantity()))
                            .setScale(2, RoundingMode.HALF_UP)
            );
        }
        order.setStatus(OrderStatus.DRAFT);

        Order saved = orderRepository.save(order);
        return OrderResponse.from(saved, shippingMethodCatalog.labelOf(saved.getShippingMethod()));
    }

    /**
     * 编辑订单：只有草稿和已预报订单允许编辑。
     * 已预报订单修改运输方式或国家时，需要重新预报，因此状态退回草稿。
     * 订单号保持不变。
     */
    @Transactional
    public OrderResponse update(Long id, UpdateOrderRequest request) {
        Order order = requireOrder(id);
        if (!order.getStatus().isEditable()) {
            throw new IllegalArgumentException("当前订单状态不允许编辑");
        }

        shippingMethodCatalog.validate(request.shippingMethod(), request.recipientCountry());

        String shippingMethod = request.shippingMethod().trim().toUpperCase(Locale.ROOT);
        String recipientCountry = request.recipientCountry().trim().toUpperCase(Locale.ROOT);
        boolean logisticsChanged = !shippingMethod.equals(order.getShippingMethod())
                || !recipientCountry.equals(order.getRecipientCountry());

        order.setShippingMethod(shippingMethod);
        order.setRecipientCountry(recipientCountry);
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
        order.setRemark(trimToNull(request.remark()));

        if (order.getStatus() == OrderStatus.FORECAST && logisticsChanged) {
            order.setStatus(OrderStatus.DRAFT);
        }

        Order saved = orderRepository.save(order);
        return OrderResponse.from(saved, shippingMethodCatalog.labelOf(saved.getShippingMethod()));
    }

    /**
     * 预报订单：只允许草稿，且必须补全订单信息。
     */
    @Transactional
    public OrderResponse forecast(Long id) {
        Order order = requireOrder(id);
        if (!order.getStatus().isForecastable()) {
            throw new IllegalArgumentException("只有草稿订单可以预报");
        }

        ensureCompleteForForecast(order);
        order.setStatus(OrderStatus.FORECAST);

        Order saved = orderRepository.save(order);
        return OrderResponse.from(saved, shippingMethodCatalog.labelOf(saved.getShippingMethod()));
    }

    private Order requireOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private void ensureCompleteForForecast(Order order) {
        if (!StringUtils.hasText(order.getCustomerName())
                || !StringUtils.hasText(order.getCustomerPhone())
                || !StringUtils.hasText(order.getProductName())
                || order.getQuantity() == null
                || order.getQuantity() <= 0
                || order.getUnitPrice() == null
                || order.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("请先补全订单信息后再预报");
        }

        shippingMethodCatalog.validate(order.getShippingMethod(), order.getRecipientCountry());
        order.setTotalAmount(
                order.getUnitPrice()
                        .multiply(BigDecimal.valueOf(order.getQuantity()))
                        .setScale(2, RoundingMode.HALF_UP)
        );
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generateOrderNo() {
        String timePart = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "ORD" + timePart + randomPart;
    }
}
