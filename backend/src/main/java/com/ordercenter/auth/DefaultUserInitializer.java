package com.ordercenter.auth;

import com.ordercenter.auth.entity.AppUser;
import com.ordercenter.auth.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 暂时不开放注册，系统启动时创建一个默认账号。
 * 生产环境请通过 AUTH_DEFAULT_PASSWORD 覆盖默认密码。
 */
@Component
public class DefaultUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultUserInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;

    public DefaultUserInitializer(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthProperties properties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = properties.getDefaultUsername();
        if (userRepository.existsByUsername(username)) {
            return;
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(properties.getDefaultPassword()));
        user.setDisplayName(properties.getDefaultDisplayName());
        user.setRole(properties.getDefaultRole());
        user.setEnabled(true);
        userRepository.save(user);

        log.warn("已创建默认登录账号：{}，请在生产环境修改默认密码", username);
    }
}
