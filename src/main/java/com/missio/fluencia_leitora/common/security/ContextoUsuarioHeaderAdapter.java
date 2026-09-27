package com.missio.fluencia_leitora.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Implementação provisória de {@link ContextoUsuarioPort} que lê os headers
 * HTTP {@code X-Perfil} e {@code X-Professor-Id} da requisição atual.
 *
 * <p>Este adapter existe só até a feature {@code autenticacao-perfis} entregar
 * um bean real, baseado em token/sessão. Quando isso acontecer, o bean real
 * deve substituir este (ex.: via {@code @Primary}), sem que os consumidores de
 * {@link ContextoUsuarioPort} precisem mudar.
 */
@Component
@RequestScope
public class ContextoUsuarioHeaderAdapter implements ContextoUsuarioPort {

    static final String HEADER_PERFIL = "X-Perfil";
    static final String HEADER_PROFESSOR_ID = "X-Professor-Id";

    private final HttpServletRequest request;

    public ContextoUsuarioHeaderAdapter(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public Perfil perfilAtual() {
        String header = request.getHeader(HEADER_PERFIL);
        if (header == null || header.isBlank()) {
            return Perfil.COORDENADOR;
        }
        return Perfil.valueOf(header);
    }

    @Override
    public Long professorIdAtual() {
        String header = request.getHeader(HEADER_PROFESSOR_ID);
        if (header == null || header.isBlank()) {
            return null;
        }
        return Long.valueOf(header);
    }
}
