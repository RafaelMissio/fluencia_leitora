package com.missio.fluencia_leitora.autenticacao;

import com.missio.fluencia_leitora.common.security.Perfil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AUTH-14 + edge case "sem usuários e sem APP_ADMIN_EMAIL": com o banco
 * vazio e as variáveis definidas cria um COORDENADOR ativo com hash BCrypt;
 * sem as variáveis loga WARN "Nenhum usuário cadastrado" e segue sem lançar;
 * com usuários já cadastrados não faz nada.
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AdminBootstrapTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private UsuarioRepository usuarioRepository;

    private void executar(String email, String senha) throws Exception {
        new AdminBootstrap(usuarioRepository, ENCODER, email, senha).run(new DefaultApplicationArguments());
    }

    @Test
    void bancoVazioComVariaveisDefinidasCriaCoordenadorAtivo() throws Exception {
        when(usuarioRepository.count()).thenReturn(0L);

        executar("Admin@Escola.com", "SenhaAdmin#1");

        ArgumentCaptor<Usuario> criado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(criado.capture());
        assertEquals("admin@escola.com", criado.getValue().getEmail());
        assertEquals(Perfil.COORDENADOR, criado.getValue().getPerfil());
        assertNull(criado.getValue().getProfessorId());
        assertTrue(criado.getValue().isAtivo());
        assertTrue(ENCODER.matches("SenhaAdmin#1", criado.getValue().getSenhaHash()));
    }

    @Test
    void bancoVazioSemVariaveisLogaWarnENaoLanca(CapturedOutput output) {
        when(usuarioRepository.count()).thenReturn(0L);

        assertDoesNotThrow(() -> executar("", ""));

        verify(usuarioRepository, never()).save(any());
        assertTrue(output.getAll().contains("WARN"), output.getAll());
        assertTrue(output.getAll().contains("Nenhum usuário cadastrado"), output.getAll());
    }

    @Test
    void bancoComUsuariosNaoFazNada() throws Exception {
        when(usuarioRepository.count()).thenReturn(3L);

        executar("admin@escola.com", "SenhaAdmin#1");

        verify(usuarioRepository, never()).save(any());
    }
}
