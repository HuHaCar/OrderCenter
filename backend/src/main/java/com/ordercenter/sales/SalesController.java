package com.ordercenter.sales;

import com.ordercenter.common.ApiResponse;
import com.ordercenter.common.PageResult;
import com.ordercenter.sales.dto.SalesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "业务员接口")
@RestController
@RequestMapping("/api")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @Operation(summary = "分页查询业务员")
    @GetMapping("/querySalesList")
    public ApiResponse<PageResult<SalesResponse>> page(
            @Parameter(description = "页码，从 1 开始")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "每页条数，最大 200")
            @RequestParam(defaultValue = "100") int size,

            @Parameter(description = "工号、业务员姓名")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "级别：A/B/C/D")
            @RequestParam(required = false) String lever
    ) {
        return ApiResponse.ok(
                salesService.page(
                        page,
                        size,
                        keyword,
                        SalesLever.fromCode(lever)
                )
        );
    }
}
