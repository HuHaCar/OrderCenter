package com.ordercenter.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "请输入账号")
        @Size(max = 50, message = "账号长度不能超过 50 个字符")
        String username,

        @NotBlank(message = "请输入密码")
        @Size(max = 100, message = "密码长度不能超过 100 个字符")
        String password
) {
}
