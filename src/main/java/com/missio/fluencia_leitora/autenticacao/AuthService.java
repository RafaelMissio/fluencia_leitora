package com.missio.fluencia_leitora.autenticacao;

import com.missio.fluencia_leitora.common.security.JwtService;
import com.missio.fluencia_leitora.common.security.Perfil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * AUTH-01..AUTH-05: login por e-mail e senha. E-mail inexistente, usuário
 * inativo e senha errada produzem o mesmo erro genérico; 5 falhas seguidas
 * bloqueiam a conta por 15 minutos; o sucesso zera o contador. A senha nunca
 * é logada.
 */
@Service
public class AuthService {

    static final String MENSAGEM_CREDENCIAIS_INVALIDAS = "Credenciais inválidas";
    static final int LIMITE_FALHAS = 5;
    static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(15);

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResult login(String email, String senhaPlana) {
        Optional<Usuario> encontrado = usuarioRepository.findByEmailIgnoreCase(email);
        if (encontrado.isEmpty() || !encontrado.get().isAtivo()) {
            log.warn("Falha de login: credenciais inválidas para {}", email);
            return credenciaisInvalidas();
        }
        Usuario usuario = encontrado.get();

        Instant agora = Instant.now();
        if (usuario.getBloqueadoAte() != null && usuario.getBloqueadoAte().isAfter(agora)) {
            long segundos = (long) Math.ceil(Duration.between(agora, usuario.getBloqueadoAte()).toMillis() / 1000.0);
            log.warn("Login bloqueado para {} por mais {}s", email, segundos);
            return new LoginResult.Bloqueado(segundos);
        }

        if (!passwordEncoder.matches(senhaPlana, usuario.getSenhaHash())) {
            usuarioRepository.registrarFalha(usuario.getId(), agora.plus(DURACAO_BLOQUEIO), LIMITE_FALHAS);
            log.warn("Falha de login: senha incorreta para {}", email);
            return credenciaisInvalidas();
        }

        usuarioRepository.zerarFalhas(usuario.getId());
        log.info("Login realizado com sucesso para {}", email);
        return new LoginResult.Sucesso(
                jwtService.emitir(usuario.getId()),
                JwtService.VALIDADE.toSeconds(),
                usuario.getPerfil(),
                usuario.getProfessorId());
    }

    private static LoginResult credenciaisInvalidas() {
        return new LoginResult.CredenciaisInvalidas(MENSAGEM_CREDENCIAIS_INVALIDAS);
    }

    /** Resultado do login: sucesso, credenciais inválidas (401) ou conta bloqueada (429). */
    public sealed interface LoginResult {

        record Sucesso(String accessToken, long expiresIn, Perfil perfil, Long professorId) implements LoginResult {
        }

        record CredenciaisInvalidas(String mensagem) implements LoginResult {
        }

        record Bloqueado(long segundosRestantes) implements LoginResult {
        }
    }
}
