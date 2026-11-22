package com.ordercenter.sales;

import com.ordercenter.common.PageResult;
import com.ordercenter.sales.performance.SalesPerformanceView;
import com.ordercenter.sales.dto.SalesResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

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
                ? keyword.trim()
                : null;
        String currentMonth = YearMonth.now(ZoneId.of("Asia/Shanghai")).toString();

        Page<SalesPerformanceView> result =
                salesRepository.pageWithCurrentPerformance(
                        currentMonth,
                        safeKeyword,
                        lever == null ? null : lever.getCode(),
                        PageRequest.of(safePage - 1, safeSize)
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
