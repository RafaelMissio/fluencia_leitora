package com.missio.fluencia_leitora.autenticacao;

import com.missio.fluencia_leitora.autenticacao.UsuarioService.UsuarioResumo;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.security.Perfil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AUTH-11/AUTH-12/AUTH-13: criar valida perfil x professorId (422), e-mail
 * único sem diferenciar maiúsculas (409 EMAIL_DUPLICADO) e senha com no
 * mínimo 8 caracteres (422), grava só o hash BCrypt e não devolve senha nem
 * hash; alterarSenha grava o novo hash e desbloqueia a conta.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProfessorRepository professorRepository;

    private UsuarioService service() {
        return new UsuarioService(usuarioRepository, professorRepository, ENCODER);
    }

    private static Professor professor(Long id, boolean ativo) {
        Professor professor = new Professor("Maria Silva");
        ReflectionTestUtils.setField(professor, "id", id);
        professor.setAtivo(ativo);
        return professor;
    }

    private static void assertErro(HttpStatus status, String code, Runnable acao) {
        BusinessException exception = assertThrows(BusinessException.class, acao::run);
        assertEquals(status, exception.getStatus());
        assertEquals(code, exception.getCode());
    }

    @Test
    void criarProfessorValidoGravaUsuarioAtivoComHashBCryptESemDevolverSenhaNemHash() {
        when(usuarioRepository.findByEmailIgnoreCase("Prof@Escola.com")).thenReturn(Optional.empty());
        when(professorRepository.findById(7L)).thenReturn(Optional.of(professor(7L, true)));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            ReflectionTestUtils.setField(usuario, "id", 99L);
            return usuario;
        });

        UsuarioResumo resumo = service().criar("Prof@Escola.com", "SenhaForte1", Perfil.PROFESSOR, 7L);

        assertEquals(new UsuarioResumo(99L, "prof@escola.com", Perfil.PROFESSOR, 7L, true), resumo);
        assertTrue(Arrays.stream(UsuarioResumo.class.getRecordComponents())
                .noneMatch(c -> c.getName().toLowerCase().contains("senha")));
        ArgumentCaptor<Usuario> gravado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(gravado.capture());
        assertFalse(gravado.getValue().getSenhaHash().contains("SenhaForte1"));
        assertTrue(ENCODER.matches("SenhaForte1", gravado.getValue().getSenhaHash()));
    }

    @Test
    void professorSemProfessorIdValidoEAtivoRetorna422() {
        when(usuarioRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(professorRepository.findById(8L)).thenReturn(Optional.empty());
        when(professorRepository.findById(9L)).thenReturn(Optional.of(professor(9L, false)));

        assertErro(HttpStatus.UNPROCESSABLE_ENTITY, "PROFESSOR_ID_INVALIDO",
                () -> service().criar("a@escola.com", "SenhaForte1", Perfil.PROFESSOR, null));
        assertErro(HttpStatus.UNPROCESSABLE_ENTITY, "PROFESSOR_ID_INVALIDO",
                () -> service().criar("a@escola.com", "SenhaForte1", Perfil.PROFESSOR, 8L));
        assertErro(HttpStatus.UNPROCESSABLE_ENTITY, "PROFESSOR_ID_INVALIDO",
                () -> service().criar("a@escola.com", "SenhaForte1", Perfil.PROFESSOR, 9L));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void coordenadorComProfessorIdRetorna422() {
        when(usuarioRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());

        assertErro(HttpStatus.UNPROCESSABLE_ENTITY, "PROFESSOR_ID_INVALIDO",
                () -> service().criar("c@escola.com", "SenhaForte1", Perfil.COORDENADOR, 7L));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void emailJaEmUsoSemDiferenciarMaiusculasRetorna409EmailDuplicado() {
        when(usuarioRepository.findByEmailIgnoreCase("COORD@ESCOLA.COM"))
                .thenReturn(Optional.of(new Usuario("coord@escola.com", "hash", Perfil.COORDENADOR, null)));

        assertErro(HttpStatus.CONFLICT, "EMAIL_DUPLICADO",
                () -> service().criar("COORD@ESCOLA.COM", "SenhaForte1", Perfil.COORDENADOR, null));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void senhaComMenosDe8CaracteresRetorna422() {
        assertErro(HttpStatus.UNPROCESSABLE_ENTITY, "SENHA_INVALIDA",
                () -> service().criar("c@escola.com", "1234567", Perfil.COORDENADOR, null));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void senhaComExatamente8CaracteresEAceita() {
        when(usuarioRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            ReflectionTestUtils.setField(usuario, "id", 1L);
            return usuario;
        });

        UsuarioResumo resumo = service().criar("c@escola.com", "12345678", Perfil.COORDENADOR, null);

        assertEquals(Perfil.COORDENADOR, resumo.perfil());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void alterarSenhaGravaNovoHashEDesbloqueiaAConta() {
        Usuario bloqueado = new Usuario("p@escola.com", ENCODER.encode("SenhaAntiga1"), Perfil.COORDENADOR, null);
        ReflectionTestUtils.setField(bloqueado, "tentativasFalhas", 5);
        ReflectionTestUtils.setField(bloqueado, "bloqueadoAte", Instant.now().plusSeconds(600));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(bloqueado));

        service().alterarSenha(3L, "SenhaNova123");

        verify(usuarioRepository).save(bloqueado);
        assertTrue(ENCODER.matches("SenhaNova123", bloqueado.getSenhaHash()));
        assertFalse(ENCODER.matches("SenhaAntiga1", bloqueado.getSenhaHash()));
        assertEquals(0, bloqueado.getTentativasFalhas());
        assertNull(bloqueado.getBloqueadoAte());
    }

    @Test
    void alterarSenhaComMenosDe8CaracteresRetorna422SemGravar() {
        assertErro(HttpStatus.UNPROCESSABLE_ENTITY, "SENHA_INVALIDA", () -> service().alterarSenha(3L, "curta"));
        verify(usuarioRepository, never()).save(any());
    }
}
