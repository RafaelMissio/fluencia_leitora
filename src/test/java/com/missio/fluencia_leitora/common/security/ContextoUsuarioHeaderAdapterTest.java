package com.missio.fluencia_leitora.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies ContextoUsuarioHeaderAdapter reads X-Perfil/X-Professor-Id and
 * falls back to the documented defaults when headers are absent.
 */
class ContextoUsuarioHeaderAdapterTest {

    @Test
    void coordenadorSemHeaderDeProfessorRetornaProfessorIdNulo() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Perfil", "COORDENADOR");

        ContextoUsuarioHeaderAdapter adapter = new ContextoUsuarioHeaderAdapter(request);

        assertEquals(Perfil.COORDENADOR, adapter.perfilAtual());
        assertNull(adapter.professorIdAtual());
    }

    @Test
    void professorComProfessorIdRetornaPerfilEIdCorretos() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Perfil", "PROFESSOR");
        request.addHeader("X-Professor-Id", "42");

        ContextoUsuarioHeaderAdapter adapter = new ContextoUsuarioHeaderAdapter(request);

        assertEquals(Perfil.PROFESSOR, adapter.perfilAtual());
        assertEquals(42L, adapter.professorIdAtual());
    }

    @Test
    void ausenciaDeHeadersUsaCoordenadorComoPadraoEProfessorIdNulo() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ContextoUsuarioHeaderAdapter adapter = new ContextoUsuarioHeaderAdapter(request);

        assertEquals(Perfil.COORDENADOR, adapter.perfilAtual());
        assertNull(adapter.professorIdAtual());
    }
}
