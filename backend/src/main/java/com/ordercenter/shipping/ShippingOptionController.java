package com.ordercenter.shipping;

import com.ordercenter.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "运输方式")
@RestController
@RequestMapping("/api")
public class ShippingOptionController {

    private final ShippingMethodCatalog shippingMethodCatalog;

    public ShippingOptionController(ShippingMethodCatalog shippingMethodCatalog) {
        this.shippingMethodCatalog = shippingMethodCatalog;
    }

    @Operation(summary = "查询运输方式及可到国家")
    @GetMapping("/shippingOptions")
    public ApiResponse<List<ShippingOptionResponse>> list() {
        return ApiResponse.ok(shippingMethodCatalog.options());
    }
}
