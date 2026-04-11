package com.sharehub.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

class EnvironmentGuardTest {

    private static final DefaultApplicationArguments EMPTY_ARGS = new DefaultApplicationArguments(new String[0]);

    @Test
    void shouldRejectDevAdminTokenInProduction() {
        EnvironmentGuard guard = new EnvironmentGuard(environmentWith("POSTGRES_PASSWORD", "secret"));
        ReflectionTestUtils.setField(guard, "appEnv", "production");
        ReflectionTestUtils.setField(guard, "adminDevTokenEnabled", true);
        ReflectionTestUtils.setField(guard, "datasourceUrl", "jdbc:postgresql://db:5432/sharehub_prod");

        assertThatThrownBy(() -> guard.run(EMPTY_ARGS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("production 环境禁止开启 SHAREHUB_ADMIN_DEV_TOKEN_ENABLED");
    }

    @Test
    void shouldRejectNonPostgresDatasourceInProduction() {
        EnvironmentGuard guard = new EnvironmentGuard(environmentWith("POSTGRES_PASSWORD", "secret"));
        ReflectionTestUtils.setField(guard, "appEnv", "production");
        ReflectionTestUtils.setField(guard, "adminDevTokenEnabled", false);
        ReflectionTestUtils.setField(guard, "datasourceUrl", "jdbc:h2:mem:sharehub");

        assertThatThrownBy(() -> guard.run(EMPTY_ARGS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("production 环境必须使用 PostgreSQL 数据源");
    }

    @Test
    void shouldRejectProductionDatabaseInTestEnvironment() {
        EnvironmentGuard guard = new EnvironmentGuard(environmentWith("POSTGRES_PASSWORD", "secret"));
        ReflectionTestUtils.setField(guard, "appEnv", "test");
        ReflectionTestUtils.setField(guard, "adminDevTokenEnabled", false);
        ReflectionTestUtils.setField(guard, "datasourceUrl", "jdbc:postgresql://db:5432/sharehub_prod");

        assertThatThrownBy(() -> guard.run(EMPTY_ARGS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("test 环境禁止连接生产数据库");
    }

    @Test
    void shouldRequirePostgresPasswordInCloudDev() {
        EnvironmentGuard guard = new EnvironmentGuard(environmentWith("POSTGRES_PASSWORD", ""));
        ReflectionTestUtils.setField(guard, "appEnv", "cloud-dev");
        ReflectionTestUtils.setField(guard, "adminDevTokenEnabled", true);
        ReflectionTestUtils.setField(guard, "datasourceUrl", "jdbc:postgresql://db:5432/sharehub_cloud_dev");

        assertThatThrownBy(() -> guard.run(EMPTY_ARGS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("cloud-dev 环境缺少 POSTGRES_PASSWORD");
    }

    @Test
    void shouldAllowSupportedConfiguration() {
        EnvironmentGuard guard = new EnvironmentGuard(environmentWith("POSTGRES_PASSWORD", "secret"));
        ReflectionTestUtils.setField(guard, "appEnv", "cloud-dev");
        ReflectionTestUtils.setField(guard, "adminDevTokenEnabled", true);
        ReflectionTestUtils.setField(guard, "datasourceUrl", "jdbc:postgresql://db:5432/sharehub_cloud_dev");

        assertThatCode(() -> guard.run(EMPTY_ARGS)).doesNotThrowAnyException();
    }

    private Environment environmentWith(String key, String value) {
        org.springframework.mock.env.MockEnvironment environment = new org.springframework.mock.env.MockEnvironment();
        environment.setProperty(key, value);
        return environment;
    }
}
