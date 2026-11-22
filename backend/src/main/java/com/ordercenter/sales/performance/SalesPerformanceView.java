package com.ordercenter.sales.performance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 业务员列表投影：Sales 基础信息 + 指定月份业绩。
 */
public interface SalesPerformanceView {

    Long getId();

    String getEmployeeNo();

    String getEmployeeName();

    String getLever();

    LocalDateTime getEntryDate();

    Integer getGender();

    String getPhone();

    String getEmail();

    String getIdCardNo();

    String getRemark();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();

    BigDecimal getCurrentMonthPerformance();
}
