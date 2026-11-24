package com.ordercenter.order;

import com.ordercenter.auth.AuthPrincipal;
import com.ordercenter.common.PageResult;
import com.ordercenter.file.OrderAttachmentService;
import com.ordercenter.order.detail.OrderDetailCacheService;
import com.ordercenter.order.dto.CreateDraftOrderRequest;
import com.ordercenter.order.dto.CreateOrderRequest;
import com.ordercenter.order.dto.OrderResponse;
import com.ordercenter.order.dto.UpdateOrderRequest;
import com.ordercenter.order.log.OrderLogAction;
import com.ordercenter.order.log.OrderLogService;
import com.ordercenter.shipping.ShippingMethodCatalog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final OrderLogService orderLogService;
    private final OrderAttachmentService orderAttachmentService;
    private final OrderDetailCacheService orderDetailCacheService;

    public OrderService(
            OrderRepository orderRepository,
            ShippingMethodCatalog shippingMethodCatalog,
            OrderLogService orderLogService,
            OrderAttachmentService orderAttachmentService,
            OrderDetailCacheService orderDetailCacheService
    ) {
        this.orderRepository = orderRepository;
        this.shippingMethodCatalog = shippingMethodCatalog;
        this.orderLogService = orderLogService;
        this.orderAttachmentService = orderAttachmentService;
        this.orderDetailCacheService = orderDetailCacheService;
    }

    public PageResult<OrderResponse> page(
            int page,
            int size,
            String keyword,
            OrderStatus status,
            Long currentUserId
    ) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String safeKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;

        Specification<Order> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 数据权限：当前用户只能看到自己创建的订单。
            predicates.add(cb.equal(root.get("createBy"), currentUserId));

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
            } else {
                // 默认列表不展示软删除订单；显式筛选 C 时才显示
                predicates.add(cb.notEqual(root.get("status"), OrderStatus.DELETED));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<Order> result = orderRepository.findAll(
                specification,
                PageRequest.of(safePage - 1, safeSize, Sort.by(
                        Sort.Order.desc("updatedAt"),
                        Sort.Order.desc("createdAt")
                ))
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
        order.setCreateBy(currentUserId());

        Order saved = orderRepository.save(order);
        orderAttachmentService.attachToOrder(
                saved.getId(),
                request.attachmentIds(),
                currentUserId()
        );
        orderLogService.record(saved, OrderLogAction.CREATE,
                "创建订单，状态：" + saved.getStatus().getLabel());
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
        order.setCreateBy(currentUserId());
        if (request.quantity() != null && request.unitPrice() != null) {
            order.setTotalAmount(
                    request.unitPrice()
                            .multiply(BigDecimal.valueOf(request.quantity()))
                            .setScale(2, RoundingMode.HALF_UP)
            );
        }
        order.setStatus(OrderStatus.DRAFT);

        Order saved = orderRepository.save(order);
        orderAttachmentService.attachToOrder(
                saved.getId(),
                request.attachmentIds(),
                currentUserId()
        );
        orderLogService.record(saved, OrderLogAction.DRAFT, "保存草稿");
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

        OrderStatus beforeStatus = order.getStatus();
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
        String updateDescription = saved.getStatus() == beforeStatus
                ? "编辑订单"
                : "编辑订单，物流信息变更，状态：" + beforeStatus.getLabel()
                    + " → " + saved.getStatus().getLabel();
        orderLogService.record(saved, OrderLogAction.UPDATE, updateDescription);
        orderDetailCacheService.evictAfterCommit(saved.getId());
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
        orderLogService.record(saved, OrderLogAction.FORECAST,
                "预报订单，状态：" + OrderStatus.DRAFT.getLabel()
                    + " → " + saved.getStatus().getLabel());
        orderDetailCacheService.evictAfterCommit(saved.getId());
        return OrderResponse.from(saved, shippingMethodCatalog.labelOf(saved.getShippingMethod()));
    }

    /**
     * 批量删除，单行删除复用同一个接口。
     * 只做软删除，且选中的订单必须全部处于可删除状态。
     */
    @Transactional
    public int deleteOrders(List<Long> ids) {
        List<Long> distinctIds = ids.stream().distinct().toList();
        List<Order> orders = orderRepository.findAllById(distinctIds);

        if (orders.size() != distinctIds.size()) {
            throw new IllegalArgumentException("部分订单不存在，请刷新后重试");
        }

        if (orders.stream().anyMatch(order -> !order.getStatus().isDeletable())) {
            throw new IllegalArgumentException("选中的订单中包含不可删除的订单");
        }

        for (Order order : orders) {
            OrderStatus beforeStatus = order.getStatus();
            order.setStatus(OrderStatus.DELETED);
            orderRepository.save(order);
            orderLogService.record(order, OrderLogAction.DELETE,
                    "删除订单，状态：" + beforeStatus.getLabel()
                        + " → " + OrderStatus.DELETED.getLabel());
            orderDetailCacheService.evictAfterCommit(order.getId());
        }
        return orders.size();
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

    private static Long currentUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthPrincipal principal) {
            return principal.userId();
        }
        return null;
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
