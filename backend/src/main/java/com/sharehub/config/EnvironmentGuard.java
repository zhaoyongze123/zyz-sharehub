package com.sharehub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentGuard implements ApplicationRunner {
    private final Environment environment;

    @Value("${sharehub.app-env:local}")
    private String appEnv;

    @Value("${sharehub.admin.dev-token-enabled:false}")
    private boolean adminDevTokenEnabled;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    public EnvironmentGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if ("production".equalsIgnoreCase(appEnv)) {
            if (adminDevTokenEnabled) {
                throw new IllegalStateException("production 环境禁止开启 SHAREHUB_ADMIN_DEV_TOKEN_ENABLED");
            }
            if (!datasourceUrl.startsWith("jdbc:postgresql:")) {
                throw new IllegalStateException("production 环境必须使用 PostgreSQL 数据源");
            }
        }

        if ("test".equalsIgnoreCase(appEnv) && datasourceUrl.contains("sharehub_prod")) {
            throw new IllegalStateException("test 环境禁止连接生产数据库");
        }

        if ("cloud-dev".equalsIgnoreCase(appEnv)) {
            String postgresPassword = environment.getProperty("POSTGRES_PASSWORD", "");
            if (postgresPassword.isBlank()) {
                throw new IllegalStateException("cloud-dev 环境缺少 POSTGRES_PASSWORD");
            }
        }
    }
}
