package com.ordercenter.sales.dto;

import com.ordercenter.sales.Sales;

import java.time.LocalDateTime;

/**
 * 业务员接口响应对象。
 * 字段与 Sales 实体对应，避免直接把 JPA Entity 暴露给前端。
 *
 * 注意：idCardNo 属于敏感信息。如果列表接口不需要展示，
 * 建议在列表查询时不要返回该字段，或者单独做脱敏处理。
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static SalesResponse from(Sales sales) {
        return new SalesResponse(
                sales.getId(),
                sales.getEmployeeNo(),
                sales.getEmployeeName(),
                sales.getLever(),
                sales.getEntryDate(),
                sales.getGender() == null ? null : sales.getGender().intValue(),
                sales.getPhone(),
                sales.getEmail(),
                sales.getIdCardNo(),
                sales.getRemark(),
                sales.getCreatedAt(),
                sales.getUpdatedAt()
        );
    }
}
