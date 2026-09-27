package com.missio.fluencia_leitora;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * SPEC_DEVIATION: replaces the Spring Initializr placeholder
 * FluenciaLeitoraApplicationTests (a bare @SpringBootTest). T1 wires a real
 * spring.datasource.* configuration, so a plain @SpringBootTest can no longer
 * load context without a reachable database. Renamed to *IT so it runs under
 * Failsafe (full gate, Docker via Testcontainers) instead of Surefire (quick
 * gate, no Docker), matching this project's own Test/IT convention.
 * Reason: keeps the context-load smoke test meaningful instead of deleting it.
 */
@SpringBootTest
@Testcontainers
class FluenciaLeitoraApplicationIT {

    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("APP_JWT_SECRET", () -> "segredo-de-integracao-com-mais-de-32-bytes");
    }

    @Test
    void contextLoads() {
    }
}
