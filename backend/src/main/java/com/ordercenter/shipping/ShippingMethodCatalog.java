package com.ordercenter.shipping;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 固定的运输方式与收件国家覆盖关系。
 * 新增/调整运输方式时，只维护这里的数据。
 */
@Component
public class ShippingMethodCatalog {

    private static final Pattern COUNTRY_CODE = Pattern.compile("^[A-Z]{2}$");

    private static final Set<String> CHINA = Set.of("CN");
    private static final Set<String> CHINA_US = Set.of("CN", "US");
    private static final Set<String> CHINA_CANADA = Set.of("CN", "CA");

    private static final Set<String> MIDDLE_EAST = Set.of(
            "AE", "BH", "EG", "IL", "IQ", "IR", "JO", "KW",
            "LB", "OM", "PS", "QA", "SA", "SY", "TR", "YE"
    );

    private static final Set<String> EUROPE = Set.of(
            "AD", "AL", "AT", "AX", "BA", "BE", "BG", "BY", "CH", "CY",
            "CZ", "DE", "DK", "EE", "ES", "FI", "FO", "FR", "GB", "GG",
            "GI", "GR", "HR", "HU", "IE", "IM", "IS", "IT", "JE", "LI",
            "LT", "LU", "LV", "MC", "MD", "ME", "MK", "MT", "NL", "NO",
            "PL", "PT", "RO", "RS", "RU", "SE", "SI", "SJ", "SK", "SM",
            "UA", "VA", "XK"
    );

    private static final Set<String> SOUTHEAST_ASIA = Set.of(
            "BN", "ID", "KH", "LA", "MM", "MY", "PH", "SG", "TH", "TL", "VN"
    );

    private static final Set<String> ISO_COUNTRIES = buildIsoCountries();

    private record Method(String code, String name, boolean allCountries, Set<String> countryCodes) {
    }

    private final List<Method> methods;
    private final Map<String, Method> byCode;

    public ShippingMethodCatalog() {
        List<Method> list = new ArrayList<>();

        list.add(limited("ZDZXR", "中邮专线", MIDDLE_EAST));
        list.add(all("THZXR", "云途全球专线挂号（特惠带电）"));
        list.add(all("BKPHR", "云途全球专线挂号（标快普货）"));
        list.add(limited("DNETK", "东南E特快", CHINA));
        list.add(limited("CNDWA", "华南快递小包平邮(DG)", CHINA));
        list.add(limited("EUB-SZ", "省内EUB", CHINA));
        list.add(limited("CNDWR", "华南快递小包挂号(DG)", CHINA));
        list.add(all("FZZXR", "云途全球服装专线挂号"));
        list.add(limited("FRZXRT", "法国专线挂号(退货)", EUROPE));
        list.add(all("BKZXR", "云途全球专线挂号（标快带电）"));
        list.add(all("THPHR", "云途全球专线挂号（特惠普货）"));
        list.add(all("THPHU", "云途全球专线平邮（特惠普货）"));
        list.add(all("THZXU", "云途全球专线平邮（特惠带电）"));
        list.add(limited("XMEUB", "东南EUB", CHINA));
        list.add(limited("HXFEDEX-02", "华西FedEx大货", CHINA));
        list.add(limited("HXFEDEXPAK", "华西FedEx-PAK", CHINA));
        list.add(limited("PK0048", "华西FEDEX", CHINA));
        list.add(limited("UPSQY-A", "华西UPS大货", CHINA));
        list.add(all("MUZXR", "云途全球化妆品类专线挂号"));
        list.add(limited("SZEMS", "省内EMS", CHINA));
        list.add(limited("SNETK", "省内E特快", CHINA));
        list.add(all("DHZXR", "云途大货专线挂号"));
        list.add(limited("USBZPHR", "中美标准普货", CHINA_US));
        list.add(limited("CALPR", "加拿大/大包专线挂号", CHINA_CANADA));
        list.add(all("WMBKZXR", "WM云途全球专线挂号（标快带电）"));
        list.add(all("WMBKPHR", "WM云途全球专线挂号（标快普货）"));
        list.add(all("WMBZPHR", "WM云途中美标准普货"));
        list.add(all("ETTHPHR", "Etsy云途全球专线挂号（特惠普货）"));
        list.add(all("HKDHL", "DHL快递（香港）"));
        list.add(limited("TGTZXR", "泰国云途全球专线挂号", SOUTHEAST_ASIA));
        list.add(limited("VNTHZXR", "越南云途全球专线挂号", SOUTHEAST_ASIA));
        list.add(all("PDBZPHR", "PD云途全球专线挂号（标快普货）"));

        this.methods = List.copyOf(list);
        this.byCode = methods.stream()
                .collect(Collectors.toUnmodifiableMap(Method::code, Function.identity()));
    }

    private static Method all(String code, String name) {
        return new Method(code, name, true, Set.of());
    }

    private static Method limited(String code, String name, Set<String> countryCodes) {
        return new Method(code, name, false, Set.copyOf(countryCodes));
    }

    public List<ShippingOptionResponse> options() {
        return methods.stream()
                .map(method -> new ShippingOptionResponse(
                        method.code(),
                        method.name(),
                        method.allCountries(),
                        method.countryCodes().stream().sorted().toList()
                ))
                .toList();
    }

    /**
     * 校验运输方式与收件国家的组合是否合法。
     */
    public void validate(String methodCode, String countryCode) {
        if (!StringUtils.hasText(methodCode)) {
            throw new IllegalArgumentException("请选择运输方式");
        }

        Method method = byCode.get(methodCode.trim().toUpperCase(Locale.ROOT));
        if (method == null) {
            throw new IllegalArgumentException("无效的运输方式：" + methodCode);
        }

        if (!StringUtils.hasText(countryCode)) {
            throw new IllegalArgumentException("请选择收件国家");
        }

        String country = countryCode.trim().toUpperCase(Locale.ROOT);
        if (!COUNTRY_CODE.matcher(country).matches() || !ISO_COUNTRIES.contains(country)) {
            throw new IllegalArgumentException("无效的收件国家：" + countryCode);
        }

        if (!method.allCountries() && !method.countryCodes().contains(country)) {
            throw new IllegalArgumentException("该运输方式暂不支持该收件国家：" + country);
        }
    }

    public String labelOf(String methodCode) {
        Method method = byCode.get(methodCode);
        return method == null ? methodCode : method.name();
    }

    private static Set<String> buildIsoCountries() {
        Set<String> codes = new HashSet<>(Arrays.asList(Locale.getISOCountries()));
        // Locale 的 ISO 列表不包含科索沃，前端完整的 ISO 数据中包含 XK
        codes.add("XK");
        return Collections.unmodifiableSet(codes);
    }
}
