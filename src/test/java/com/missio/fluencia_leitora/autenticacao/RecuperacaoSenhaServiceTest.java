package com.missio.fluencia_leitora.autenticacao;

import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.security.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Recuperação de senha: só contas ativas recebem token; o e-mail traz o link com
 * o token em texto e o banco guarda só o hash; o token é de uso único e expira.
 */
@ExtendWith(MockitoExtension.class)
class RecuperacaoSenhaServiceTest {

    private static final String EMAIL = "prof@escola.com";
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RedefinicaoSenhaTokenRepository tokenRepository;
    @Mock
    private ObjectProvider<JavaMailSender> mailProvider;
    @Mock
    private JavaMailSender mailSender;

    private RecuperacaoSenhaService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new RecuperacaoSenhaService(
                usuarioRepository, tokenRepository, ENCODER, mailProvider, "http://app", "no-reply@x");
        usuario = new Usuario(EMAIL, ENCODER.encode("SenhaAntiga#1"), Perfil.PROFESSOR, 1L);
        ReflectionTestUtils.setField(usuario, "id", 7L);
    }

    @Test
    void solicitar_comEmailAtivo_salvaSoOHashEEnviaLinkComToken() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(mailProvider.getIfAvailable()).thenReturn(mailSender);

        service.solicitar(EMAIL);

        ArgumentCaptor<RedefinicaoSenhaToken> salvo = ArgumentCaptor.forClass(RedefinicaoSenhaToken.class);
        verify(tokenRepository).apagarPendentes(7L);
        verify(tokenRepository).save(salvo.capture());
        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());

        assertEquals(EMAIL, mail.getValue().getTo()[0]);
        Matcher m = Pattern.compile("http://app/redefinir-senha\\?token=([\\w-]+)").matcher(mail.getValue().getText());
        assertTrue(m.find());
        String token = m.group(1);
        assertEquals(7L, salvo.getValue().getUsuarioId());
        assertFalse(ReflectionTestUtils.getField(salvo.getValue(), "tokenHash").toString().contains(token));
    }

    @Test
    void solicitar_semSmtp_naoFalha() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(mailProvider.getIfAvailable()).thenReturn(null);

        service.solicitar(EMAIL);

        verify(tokenRepository).save(any());
    }

    @Test
    void solicitar_emailInexistenteOuInativo_naoGeraTokenNemEmail() {
        when(usuarioRepository.findByEmailIgnoreCase("nao@existe.com")).thenReturn(Optional.empty());
        service.solicitar("nao@existe.com");

        usuario.setAtivo(false);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        service.solicitar(EMAIL);

        verify(tokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void redefinir_comTokenValido_trocaSenhaEMarcaUsado() {
        String token = tokenEmitido();
        RedefinicaoSenhaToken registro = lastSaved;
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(registro));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        service.redefinir(token, "NovaSenha#123");

        assertTrue(ENCODER.matches("NovaSenha#123", usuario.getSenhaHash()));
        assertFalse(registro.estaValido(Instant.now()));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void redefinir_tokenJaUsado_ehRejeitado() {
        RedefinicaoSenhaToken registro = new RedefinicaoSenhaToken(7L, "h", Instant.now().plusSeconds(60));
        registro.marcarUsado(Instant.now());
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(registro));

        BusinessException e = assertThrows(BusinessException.class, () -> service.redefinir("t", "NovaSenha#123"));

        assertEquals("TOKEN_INVALIDO", e.getCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void redefinir_tokenExpiradoOuDesconhecido_ehRejeitado() {
        RedefinicaoSenhaToken expirado = new RedefinicaoSenhaToken(7L, "h", Instant.now().minusSeconds(1));
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expirado), Optional.empty());

        assertThrows(BusinessException.class, () -> service.redefinir("t", "NovaSenha#123"));
        assertThrows(BusinessException.class, () -> service.redefinir("t", "NovaSenha#123"));
    }

    @Test
    void redefinir_senhaCurta_ehRejeitadaAntesDeConsultarToken() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.redefinir("t", "curta"));

        assertEquals("SENHA_INVALIDA", e.getCode());
        verify(tokenRepository, never()).findByTokenHash(any());
    }

    /** Emite um token via {@code solicitar} e devolve o texto enviado por e-mail. */
    private String tokenEmitido() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(mailProvider.getIfAvailable()).thenReturn(mailSender);
        service.solicitar(EMAIL);
        ArgumentCaptor<RedefinicaoSenhaToken> salvo = ArgumentCaptor.forClass(RedefinicaoSenhaToken.class);
        verify(tokenRepository).save(salvo.capture());
        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        Matcher m = Pattern.compile("token=([\\w-]+)").matcher(mail.getValue().getText());
        assertTrue(m.find());
        assertNotNull(salvo.getValue());
        lastSaved = salvo.getValue();
        return m.group(1);
    }

    private RedefinicaoSenhaToken lastSaved;
}
