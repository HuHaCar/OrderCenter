package com.ordercenter.order.log;

import com.ordercenter.common.ApiResponse;
import com.ordercenter.order.log.dto.OrderLogQueryRequest;
import com.ordercenter.order.log.dto.OrderLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "订单日志")
@RestController
@RequestMapping("/api")
public class OrderLogController {

    private final OrderLogService orderLogService;

    public OrderLogController(OrderLogService orderLogService) {
        this.orderLogService = orderLogService;
    }

    @Operation(summary = "查询订单操作日志")
    @PostMapping("/queryOrderLogs")
    public ApiResponse<List<OrderLogResponse>> list(
            @Valid @RequestBody OrderLogQueryRequest request
    ) {
        return ApiResponse.ok(orderLogService.list(request.orderId()));
    }
}
