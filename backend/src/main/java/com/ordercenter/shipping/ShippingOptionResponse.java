package com.ordercenter.shipping;

import java.util.List;

/**
 * 运输方式及其可覆盖的收件国家。
 * allCountries=true 时 countryCodes 为空，表示所有国家。
 */
public record ShippingOptionResponse(
        String code,
        String name,
        boolean allCountries,
        List<String> countryCodes
) {
}
