package com.ordercenter.order;

import com.ordercenter.common.PageResult;
import com.ordercenter.order.dto.OrderResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final int MAX_PAGE_SIZE = 200;

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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
                .map(OrderResponse::from)
                .toList();

        return new PageResult<>(records, result.getTotalElements(), safePage, safeSize);
    }
}
