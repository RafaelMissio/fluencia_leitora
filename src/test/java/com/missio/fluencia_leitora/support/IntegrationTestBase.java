package com.missio.fluencia_leitora.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base class for *ControllerIT / *RepositoryIT tests: boots the full Spring
 * context against a real MySQL container (AD-007), shared by every
 * cadastros-base integration test so the Testcontainers wiring is written
 * once.
 *
 * <p>SPEC_DEVIATION: uses the Testcontainers "singleton container" pattern
 * (manual start, no {@code @Container}/{@code @Testcontainers}) instead of
 * the per-class {@code @Container} lifecycle. With {@code @Container} on a
 * static field inherited by multiple test classes, JUnit's extension stops
 * the container after each subclass's tests and restarts it for the next
 * one; under that restart churn, later test classes intermittently hit
 * "Connection refused"/HikariCP pool-timeout errors once several IT classes
 * share this base (surfaced once T8 added a third subclass). Starting the
 * container once and never stopping it - Ryuk removes it when the JVM exits
 * - avoids the restart race entirely.
 */
@SpringBootTest
public abstract class IntegrationTestBase {

    static final MySQLContainer mysql = new MySQLContainer("mysql:8.0");

    static {
        mysql.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
