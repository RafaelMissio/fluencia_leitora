package com.missio.fluencia_leitora.common.security;

import com.missio.fluencia_leitora.autenticacao.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lê {@code Authorization: Bearer <token>} e, se o token for válido e o
 * usuário ainda estiver ativo (AUTH-10), popula o SecurityContext com um
 * {@link UsuarioAutenticado} lido do banco. Caso contrário a cadeia segue
 * sem autenticação e o 401 é decidido adiante pelo Spring Security.
 *
 * <p>Não é {@code @Component}: é instanciado pelo {@code SecurityConfig} para
 * não ser registrado também como filtro de servlet comum.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIXO_BEARER)) {
            jwtService.validarERetornarUsuarioId(header.substring(PREFIXO_BEARER.length()))
                    .flatMap(usuarioRepository::findById)
                    .filter(usuario -> usuario.isAtivo())
                    .ifPresent(usuario -> {
                        UsuarioAutenticado principal = new UsuarioAutenticado(
                                usuario.getId(), usuario.getPerfil(), usuario.getProfessorId());
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil())));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        chain.doFilter(request, response);
    }
}
