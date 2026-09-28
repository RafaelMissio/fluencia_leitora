package com.missio.fluencia_leitora.common.security;

import com.missio.fluencia_leitora.autenticacao.UsuarioRepository;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cadeia stateless com JWT (AUTH-06, AUTH-15): libera só as rotas públicas
 * do spec e exige autenticação no resto. 401/403 saem no mesmo formato
 * ProblemDetail (com {@code code}) do {@code GlobalExceptionHandler}.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtService jwtService, UsuarioRepository usuarioRepository) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                "/api/v1/auth/login", "/v3/api-docs/**", "/swagger-ui/**", "/actuator/health")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) -> escreverProblema(
                                request, response, HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO",
                                "Autenticação necessária"))
                        .accessDeniedHandler((request, response, ex) -> escreverProblema(
                                request, response, HttpStatus.FORBIDDEN, "ACESSO_NEGADO",
                                "Acesso negado para o perfil do usuário")))
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService, usuarioRepository),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void escreverProblema(
            HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code, String detail)
            throws IOException {
        Map<String, Object> problema = new LinkedHashMap<>();
        problema.put("type", "about:blank");
        problema.put("title", status.getReasonPhrase());
        problema.put("status", status.value());
        problema.put("detail", detail);
        problema.put("instance", request.getRequestURI());
        problema.put("code", code);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSON.writeValueAsString(problema));
    }
}
