package com.ordercenter.sales;

import com.ordercenter.sales.performance.SalesPerformanceView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesRepository
        extends JpaRepository<Sales, Long>, JpaSpecificationExecutor<Sales> {

    /**
     * 一条 LEFT JOIN 返回业务员和指定月份的业绩。
     * performance_month 使用参数传入，可以命中唯一索引。
     */
    @Query(
            value = """
                    select
                        s.id as id,
                        s.employeeNo as employeeNo,
                        s.employeeName as employeeName,
                        s.lever as lever,
                        s.entryDate as entryDate,
                        s.gender as gender,
                        s.phone as phone,
                        s.email as email,
                        s.idCardNo as idCardNo,
                        s.remark as remark,
                        s.createdAt as createdAt,
                        s.updatedAt as updatedAt,
                        coalesce(sp.amount, 0) as currentMonthPerformance
                    from Sales s
                    left join SalesPerformance sp
                        on sp.salesId = s.id
                       and sp.performanceMonth = :month
                    where (:keyword is null
                        or lower(s.employeeNo) like lower(concat('%', :keyword, '%'))
                        or lower(s.employeeName) like lower(concat('%', :keyword, '%'))
                        or lower(s.phone) like lower(concat('%', :keyword, '%')))
                      and (:lever is null or s.lever = :lever)
                    order by s.updatedAt desc, s.createdAt desc
                    """,
            countQuery = """
                    select count(s.id)
                    from Sales s
                    where (:keyword is null
                        or lower(s.employeeNo) like lower(concat('%', :keyword, '%'))
                        or lower(s.employeeName) like lower(concat('%', :keyword, '%'))
                        or lower(s.phone) like lower(concat('%', :keyword, '%')))
                      and (:lever is null or s.lever = :lever)
                    """
    )
    Page<SalesPerformanceView> pageWithCurrentPerformance(
            @Param("month") String month,
            @Param("keyword") String keyword,
            @Param("lever") String lever,
            Pageable pageable
    );
}
