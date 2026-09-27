package com.missio.fluencia_leitora.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base class for *ControllerIT / *RepositoryIT tests: boots the full Spring
 * context against a real MySQL container (AD-007), shared by every
 * cadastros-base integration test so the Testcontainers wiring is written
 * once. The container is started once per test JVM and reused across
 * subclasses (Testcontainers singleton-container pattern).
 */
@SpringBootTest
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
