package com.missio.fluencia_leitora.autenticacao;

import com.missio.fluencia_leitora.common.error.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * "Esqueci a senha": gera um token de uso único (validade de 1 hora), envia o
 * link por e-mail e, ao receber o token, troca a senha. A solicitação nunca
 * revela se o e-mail existe. Sem SMTP configurado, o link vai apenas para o log.
 */
@Service
public class RecuperacaoSenhaService {

    static final Duration VALIDADE = Duration.ofHours(1);
    private static final int SENHA_MINIMA = 8;
    private static final Logger log = LoggerFactory.getLogger(RecuperacaoSenhaService.class);

    private final UsuarioRepository usuarioRepository;
    private final RedefinicaoSenhaTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String frontendUrl;
    private final String remetente;
    private final SecureRandom random = new SecureRandom();

    public RecuperacaoSenhaService(
            UsuarioRepository usuarioRepository, RedefinicaoSenhaTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder, ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.frontend-url}") String frontendUrl, @Value("${app.mail-from}") String remetente) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.remetente = remetente;
    }

    @Transactional
    public void solicitar(String email) {
        usuarioRepository.findByEmailIgnoreCase(email).filter(Usuario::isAtivo).ifPresent(usuario -> {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            tokenRepository.apagarPendentes(usuario.getId());
            tokenRepository.save(new RedefinicaoSenhaToken(
                    usuario.getId(), hash(token), Instant.now().plus(VALIDADE)));
            enviar(usuario.getEmail(), frontendUrl + "/redefinir-senha?token=" + token);
        });
    }

    @Transactional
    public void redefinir(String token, String novaSenha) {
        if (novaSenha == null || novaSenha.length() < SENHA_MINIMA) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "SENHA_INVALIDA", "A senha deve ter pelo menos 8 caracteres");
        }
        Instant agora = Instant.now();
        RedefinicaoSenhaToken registro = tokenRepository.findByTokenHash(hash(token))
                .filter(t -> t.estaValido(agora))
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST, "TOKEN_INVALIDO", "Link inválido ou expirado"));
        Usuario usuario = usuarioRepository.findById(registro.getUsuarioId())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST, "TOKEN_INVALIDO", "Link inválido ou expirado"));

        usuario.redefinirSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
        registro.marcarUsado(agora);
        tokenRepository.save(registro);
    }

    private void enviar(String destino, String link) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP não configurado; link de redefinição de senha para {}: {}", destino, link);
            return;
        }
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destino);
        mensagem.setSubject("Redefinição de senha - Fluência Leitora");
        mensagem.setText("Recebemos um pedido para redefinir sua senha.\n\n"
                + "Acesse o link abaixo (válido por 1 hora):\n" + link + "\n\n"
                + "Se não foi você, ignore este e-mail.");
        try {
            sender.send(mensagem);
        } catch (RuntimeException e) {
            log.error("Falha ao enviar e-mail de redefinição para {}", destino, e);
        }
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
