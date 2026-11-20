package com.ordercenter.sales;

import com.ordercenter.common.PageResult;
import com.ordercenter.sales.dto.SalesResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class SalesService {

    private static final int MAX_PAGE_SIZE = 200;

    private final SalesRepository salesRepository;

    public SalesService(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    public PageResult<SalesResponse> page(
            int page,
            int size,
            String keyword,
            SalesLever lever
    ) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String safeKeyword = StringUtils.hasText(keyword)
                ? keyword.trim().toLowerCase(Locale.ROOT)
                : null;

        Specification<Sales> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (safeKeyword != null) {
                String like = "%" + safeKeyword + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("employeeNo")), like),
                        cb.like(cb.lower(root.get("employeeName")), like)
                ));
            }

            if (lever != null) {
                // sales.lever 是 VARCHAR，存储的是 A/B/C/D，不是枚举对象。
                predicates.add(cb.equal(root.get("lever"), lever.getCode()));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<Sales> result = salesRepository.findAll(
                specification,
                PageRequest.of(
                        safePage - 1,
                        safeSize,
                        Sort.by(
                                Sort.Order.desc("updatedAt"),
                                Sort.Order.desc("createdAt")
                        )
                )
        );

        List<SalesResponse> records = result.getContent().stream()
                .map(SalesResponse::from)
                .toList();

        return new PageResult<>(
                records,
                result.getTotalElements(),
                safePage,
                safeSize
        );
    }
}
