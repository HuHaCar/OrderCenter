package com.ordercenter.order;

import com.ordercenter.common.ApiResponse;
import com.ordercenter.common.PageResult;
import com.ordercenter.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@Tag(name = "订单接口")
@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "status", "UP",
                "service", "order-center",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @Operation(summary = "分页查询订单")
    @GetMapping("/orders")
    public ApiResponse<PageResult<OrderResponse>> page(
            @Parameter(description = "页码，从 1 开始")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "每页条数，最大 200")
            @RequestParam(defaultValue = "100") int size,

            @Parameter(description = "订单号、客户姓名或商品名称")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "订单状态")
            @RequestParam(required = false) OrderStatus status
    ) {
        return ApiResponse.ok(orderService.page(page, size, keyword, status));
    }
}
