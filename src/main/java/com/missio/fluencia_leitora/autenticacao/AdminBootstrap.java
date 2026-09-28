package com.missio.fluencia_leitora.autenticacao;

import com.missio.fluencia_leitora.common.security.Perfil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * AUTH-14: na inicialização, com o banco sem usuários, cria um COORDENADOR a
 * partir de {@code APP_ADMIN_EMAIL}/{@code APP_ADMIN_PASSWORD}. Sem essas
 * variáveis, loga WARN e deixa a aplicação subir.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminSenha;

    public AdminBootstrap(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${APP_ADMIN_EMAIL:}") String adminEmail,
            @Value("${APP_ADMIN_PASSWORD:}") String adminSenha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminSenha = adminSenha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (adminEmail.isBlank() || adminSenha.isBlank()) {
            log.warn("Nenhum usuário cadastrado: defina APP_ADMIN_EMAIL e APP_ADMIN_PASSWORD para criar o coordenador");
            return;
        }
        usuarioRepository.save(new Usuario(adminEmail, passwordEncoder.encode(adminSenha), Perfil.COORDENADOR, null));
        log.info("Coordenador inicial criado: {}", adminEmail);
    }
}
