package com.ordercenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 系统使用自己的 JWT 过滤器，不需要 Spring Boot 自动生成的默认用户。
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class OrderCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderCenterApplication.class, args);
    }
}
