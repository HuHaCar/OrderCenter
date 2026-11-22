package com.ordercenter.order;

import com.ordercenter.auth.AuthPrincipal;
import com.ordercenter.common.ApiResponse;
import com.ordercenter.common.PageResult;
import com.ordercenter.order.dto.CreateDraftOrderRequest;
import com.ordercenter.order.dto.CreateOrderRequest;
import com.ordercenter.order.dto.DeleteOrdersRequest;
import com.ordercenter.order.dto.ExportOrdersRequest;
import com.ordercenter.order.dto.ForecastOrderRequest;
import com.ordercenter.order.dto.OrderResponse;
import com.ordercenter.order.dto.UpdateOrderRequest;
import com.ordercenter.order.export.ExportedFile;
import com.ordercenter.order.export.OrderExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final OrderExportService orderExportService;

    public OrderController(OrderService orderService, OrderExportService orderExportService) {
        this.orderService = orderService;
        this.orderExportService = orderExportService;
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
    @GetMapping("/queryOrderList")
    public ApiResponse<PageResult<OrderResponse>> page(
            @Parameter(description = "页码，从 1 开始")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "每页条数，最大 200")
            @RequestParam(defaultValue = "100") int size,

            @Parameter(description = "订单号、客户姓名或商品名称")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "订单状态：T/S/R/D/F/V/P/C/Q")
            @RequestParam(required = false) String status,

            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(orderService.page(
                page,
                size,
                keyword,
                OrderStatus.fromCode(status),
                principal.userId()
        ));
    }

    @Operation(summary = "新增订单")
    @PostMapping("/createOrder")
    public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(orderService.create(request));
    }

    @Operation(summary = "保存订单草稿")
    @PostMapping("/createDraftOrder")
    public ApiResponse<OrderResponse> createDraft(@Valid @RequestBody CreateDraftOrderRequest request) {
        return ApiResponse.ok(orderService.createDraft(request));
    }

    @Operation(summary = "编辑订单")
    @PostMapping("/updateOrder")
    public ApiResponse<OrderResponse> update(@Valid @RequestBody UpdateOrderRequest request) {
        return ApiResponse.ok(orderService.update(request.id(), request));
    }

    @Operation(summary = "预报订单")
    @PostMapping("/forecastOrder")
    public ApiResponse<OrderResponse> forecast(@Valid @RequestBody ForecastOrderRequest request) {
        return ApiResponse.ok(orderService.forecast(request.id()));
    }

    @Operation(summary = "批量删除订单（单行删除也复用）")
    @PostMapping("/deleteOrders")
    public ApiResponse<Integer> deleteOrders(@Valid @RequestBody DeleteOrdersRequest request) {
        return ApiResponse.ok(orderService.deleteOrders(request.ids()));
    }

    @Operation(summary = "批量导出订单（.xlsx）")
    @PostMapping(value = "/exportOrders", produces = OrderExportService.CONTENT_TYPE)
    public ResponseEntity<byte[]> exportOrders(@Valid @RequestBody ExportOrdersRequest request) {
        ExportedFile file = orderExportService.export(request.ids());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.fileName())
                                .build()
                                .toString()
                )
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType(OrderExportService.CONTENT_TYPE))
                .contentLength(file.content().length)
                .body(file.content());
    }
}
