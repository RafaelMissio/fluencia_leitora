package com.missio.fluencia_leitora.autenticacao;

import com.missio.fluencia_leitora.autenticacao.AuthService.LoginResult;
import com.missio.fluencia_leitora.common.security.JwtService;
import com.missio.fluencia_leitora.common.security.Perfil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AUTH-01..AUTH-05 + edge case "bloqueio expirado": login devolve token de 8h
 * com perfil/professorId; e-mail inexistente, senha errada e usuário inativo
 * produzem exatamente o mesmo resultado "Credenciais inválidas"; a 5ª falha
 * seguida bloqueia por 15 minutos e, enquanto bloqueado, até a senha certa
 * devolve "bloqueado" com os segundos restantes; sucesso zera o contador; a
 * senha nunca aparece no log.
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AuthServiceTest {

    private static final String EMAIL = "prof@escola.com";
    private static final String SENHA = "SenhaSecreta#123";
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;

    private AuthService service() {
        return new AuthService(usuarioRepository, ENCODER, jwtService);
    }

    private static Usuario usuario(Long id, Perfil perfil, Long professorId) {
        Usuario usuario = new Usuario(EMAIL, ENCODER.encode(SENHA), perfil, professorId);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    @Test
    void loginCertoRetornaTokenExpiresIn28800PerfilEProfessorId() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario(10L, Perfil.PROFESSOR, 7L)));
        when(jwtService.emitir(10L)).thenReturn("token-jwt");

        LoginResult resultado = service().login(EMAIL, SENHA);

        assertEquals(new LoginResult.Sucesso("token-jwt", 28800L, Perfil.PROFESSOR, 7L), resultado);
    }

    @Test
    void emailInexistenteESenhaErradaRetornamExatamenteOMesmoErroGenerico() {
        when(usuarioRepository.findByEmailIgnoreCase("naoexiste@escola.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario(10L, Perfil.PROFESSOR, 7L)));

        LoginResult inexistente = service().login("naoexiste@escola.com", SENHA);
        LoginResult senhaErrada = service().login(EMAIL, "senha-errada");

        assertEquals(new LoginResult.CredenciaisInvalidas("Credenciais inválidas"), inexistente);
        assertEquals(inexistente, senhaErrada);
        verify(jwtService, never()).emitir(anyLong());
    }

    @Test
    void usuarioInativoRetornaCredenciaisInvalidasMesmoComSenhaCerta() {
        Usuario inativo = usuario(10L, Perfil.COORDENADOR, null);
        inativo.setAtivo(false);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(inativo));

        LoginResult resultado = service().login(EMAIL, SENHA);

        assertEquals(new LoginResult.CredenciaisInvalidas("Credenciais inválidas"), resultado);
        verify(jwtService, never()).emitir(anyLong());
    }

    @Test
    void falhaDeSenhaRegistraFalhaComLimite5EBloqueioDe15Minutos() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario(10L, Perfil.PROFESSOR, 7L)));
        Instant antes = Instant.now();

        service().login(EMAIL, "senha-errada");

        ArgumentCaptor<Instant> bloqueadoAte = ArgumentCaptor.forClass(Instant.class);
        verify(usuarioRepository).registrarFalha(eq(10L), bloqueadoAte.capture(), eq(5));
        Duration duracao = Duration.between(antes, bloqueadoAte.getValue());
        assertTrue(duracao.compareTo(Duration.ofMinutes(15)) >= 0, "bloqueio deve durar 15 minutos: " + duracao);
        assertTrue(duracao.compareTo(Duration.ofMinutes(15).plusSeconds(5)) < 0, "bloqueio deve durar 15 minutos: " + duracao);
    }

    @Test
    void contaBloqueadaRetornaBloqueadoComSegundosRestantesMesmoComSenhaCerta() {
        Usuario bloqueado = usuario(10L, Perfil.PROFESSOR, 7L);
        ReflectionTestUtils.setField(bloqueado, "tentativasFalhas", 5);
        ReflectionTestUtils.setField(bloqueado, "bloqueadoAte", Instant.now().plus(Duration.ofMinutes(10)));
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(bloqueado));

        LoginResult resultado = service().login(EMAIL, SENHA);

        LoginResult.Bloqueado estado = assertInstanceOf(LoginResult.Bloqueado.class, resultado);
        assertTrue(estado.segundosRestantes() > 590 && estado.segundosRestantes() <= 600,
                "segundos restantes: " + estado.segundosRestantes());
        verify(jwtService, never()).emitir(anyLong());
        verify(usuarioRepository, never()).zerarFalhas(anyLong());
    }

    @Test
    void loginCertoZeraContadorDeFalhasInclusiveAposBloqueioExpirado() {
        Usuario comFalhas = usuario(10L, Perfil.PROFESSOR, 7L);
        ReflectionTestUtils.setField(comFalhas, "tentativasFalhas", 5);
        ReflectionTestUtils.setField(comFalhas, "bloqueadoAte", Instant.now().minusSeconds(1));
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(comFalhas));
        when(jwtService.emitir(10L)).thenReturn("token-jwt");

        LoginResult resultado = service().login(EMAIL, SENHA);

        assertInstanceOf(LoginResult.Sucesso.class, resultado);
        verify(usuarioRepository).zerarFalhas(10L);
        verify(usuarioRepository, never()).registrarFalha(anyLong(), any(), anyInt());
    }

    @Test
    void nenhumaLinhaDeLogContemASenha(CapturedOutput output) {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario(10L, Perfil.PROFESSOR, 7L)));
        when(jwtService.emitir(10L)).thenReturn("token-jwt");

        service().login(EMAIL, SENHA);
        service().login(EMAIL, "OutraSenhaErrada#999");

        assertTrue(output.getAll().contains(EMAIL), "o login deve ser logado com o e-mail");
        assertFalse(output.getAll().contains(SENHA));
        assertFalse(output.getAll().contains("OutraSenhaErrada#999"));
    }
}
