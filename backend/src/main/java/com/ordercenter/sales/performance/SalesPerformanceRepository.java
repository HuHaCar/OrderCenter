package com.ordercenter.sales.performance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface SalesPerformanceRepository
        extends JpaRepository<SalesPerformance, Long> {

    Optional<SalesPerformance> findBySalesIdAndPerformanceMonth(
            Long salesId,
            String performanceMonth
    );

    /**
     * 原子 upsert，避免“先查后写”在并发下两个请求同时 insert。
     * 唯一索引 (sales_id, performance_month) 仍然作为最终兜底。
     */
    @Modifying
    @Query(
            value = """
                    insert into sales_performance
                        (sales_id, performance_month, amount)
                    values
                        (:salesId, :month, :amount)
                    on duplicate key update
                        amount = values(amount),
                        updated_at = current_timestamp(6)
                    """,
            nativeQuery = true
    )
    int upsert(
            @Param("salesId") Long salesId,
            @Param("month") String performanceMonth,
            @Param("amount") BigDecimal amount
    );
}
