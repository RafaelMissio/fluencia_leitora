package com.missio.fluencia_leitora.common.security;

import com.missio.fluencia_leitora.common.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * AUTH-09: um PROFESSOR que acessa recurso de outro professor recebe 404
 * (não revela a existência); o dono e o COORDENADOR passam.
 */
@ExtendWith(MockitoExtension.class)
class PertencimentoProfessorGuardTest {

    @Mock
    private ContextoUsuarioPort contextoUsuario;

    private PertencimentoProfessorGuard guard;

    @BeforeEach
    void setUp() {
        guard = new PertencimentoProfessorGuard(contextoUsuario);
    }

    @Test
    void professorAcessandoRecursoDeOutroProfessorRecebe404() {
        when(contextoUsuario.perfilAtual()).thenReturn(Perfil.PROFESSOR);
        when(contextoUsuario.professorIdAtual()).thenReturn(10L);

        BusinessException ex = assertThrows(BusinessException.class, () -> guard.verificar(20L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("RECURSO_NAO_ENCONTRADO", ex.getCode());
    }

    @Test
    void professorDonoDoRecursoPassa() {
        when(contextoUsuario.perfilAtual()).thenReturn(Perfil.PROFESSOR);
        when(contextoUsuario.professorIdAtual()).thenReturn(10L);

        assertDoesNotThrow(() -> guard.verificar(10L));
    }

    @Test
    void coordenadorPassaIndependenteDoProfessorDoRecurso() {
        when(contextoUsuario.perfilAtual()).thenReturn(Perfil.COORDENADOR);
        lenient().when(contextoUsuario.professorIdAtual()).thenReturn(null);

        assertDoesNotThrow(() -> guard.verificar(20L));
        assertDoesNotThrow(() -> guard.verificar(null));
    }
}
