package com.missio.fluencia_leitora.common.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Implementação real de {@link ContextoUsuarioPort}: lê o
 * {@link UsuarioAutenticado} que o {@link JwtAuthenticationFilter} colocou no
 * SecurityContext da requisição atual.
 */
@Component
public class JwtContextoUsuarioAdapter implements ContextoUsuarioPort {

    @Override
    public Perfil perfilAtual() {
        return usuarioAtual().perfil();
    }

    @Override
    public Long professorIdAtual() {
        return usuarioAtual().professorId();
    }

    private UsuarioAutenticado usuarioAtual() {
        return (UsuarioAutenticado) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
