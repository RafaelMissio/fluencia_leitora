package com.missio.fluencia_leitora.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * O adapter real de {@link ContextoUsuarioPort} devolve o perfil e o
 * professorId do {@link UsuarioAutenticado} presente no SecurityContext.
 */
class JwtContextoUsuarioAdapterTest {

    private final JwtContextoUsuarioAdapter adapter = new JwtContextoUsuarioAdapter();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private static void autenticar(UsuarioAutenticado principal) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.perfil()))));
    }

    @Test
    void coordenadorRetornaPerfilCoordenadorEProfessorIdNulo() {
        autenticar(new UsuarioAutenticado(1L, Perfil.COORDENADOR, null));

        assertEquals(Perfil.COORDENADOR, adapter.perfilAtual());
        assertNull(adapter.professorIdAtual());
    }

    @Test
    void professorRetornaPerfilProfessorESeuProfessorId() {
        autenticar(new UsuarioAutenticado(2L, Perfil.PROFESSOR, 42L));

        assertEquals(Perfil.PROFESSOR, adapter.perfilAtual());
        assertEquals(42L, adapter.professorIdAtual());
    }

    @Test
    void usuarioIdAtualRetornaUsuarioIdDoPrincipalAutenticado() {
        autenticar(new UsuarioAutenticado(7L, Perfil.COORDENADOR, null));

        assertEquals(7L, adapter.usuarioIdAtual());
    }
}
