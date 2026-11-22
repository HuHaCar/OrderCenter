package com.ordercenter.sales.dto;

import com.ordercenter.sales.performance.SalesPerformanceView;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 业务员列表响应对象。
 * 基础信息来自 Sales，本月业绩来自 sales_performance 的 LEFT JOIN 投影。
 *
 * 注意：idCardNo 属于敏感信息，列表接口如不需要建议移除或脱敏。
 */
public record SalesResponse(
        Long id,
        String employeeNo,
        String employeeName,
        String lever,
        LocalDateTime entryDate,
        Integer gender,
        String phone,
        String email,
        String idCardNo,
        String remark,
        BigDecimal currentMonthPerformance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static SalesResponse from(SalesPerformanceView view) {
        return new SalesResponse(
                view.getId(),
                view.getEmployeeNo(),
                view.getEmployeeName(),
                view.getLever(),
                view.getEntryDate(),
                view.getGender(),
                view.getPhone(),
                view.getEmail(),
                view.getIdCardNo(),
                view.getRemark(),
                view.getCurrentMonthPerformance(),
                view.getCreatedAt(),
                view.getUpdatedAt()
        );
    }
}
