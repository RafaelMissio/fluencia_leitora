package com.missio.fluencia_leitora.autenticacao;

import com.missio.fluencia_leitora.common.security.Perfil;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AUTH-03/AUTH-12: busca por e-mail sem diferenciar maiúsculas, e-mail único
 * sem diferenciar maiúsculas, contador de falhas com bloqueio só ao atingir o
 * limite e reset do contador - contra MySQL real.
 */
@Transactional
class UsuarioRepositoryIT extends IntegrationTestBase {

    private static final int LIMITE = 5;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario novoUsuario(String email) {
        return usuarioRepository.saveAndFlush(new Usuario(email, "hash", Perfil.COORDENADOR, null));
    }

    @Test
    void findByEmailIgnoreCaseEncontraIndependenteDeMaiusculas() {
        Usuario salvo = novoUsuario("Maria.Coord@Escola.com");

        Optional<Usuario> minusculas = usuarioRepository.findByEmailIgnoreCase("maria.coord@escola.com");
        Optional<Usuario> maiusculas = usuarioRepository.findByEmailIgnoreCase("MARIA.COORD@ESCOLA.COM");

        assertTrue(minusculas.isPresent());
        assertEquals(salvo.getId(), minusculas.get().getId());
        assertTrue(maiusculas.isPresent());
        assertEquals(salvo.getId(), maiusculas.get().getId());
    }

    @Test
    void emailDuplicadoSemDiferenciarMaiusculasViolaConstraintUnica() {
        novoUsuario("duplicado@escola.com");

        assertThrows(DataIntegrityViolationException.class, () -> novoUsuario("DUPLICADO@Escola.com"));
    }

    @Test
    void registrarFalhaIncrementaESoBloqueiaAoAtingirOLimite() {
        Usuario usuario = novoUsuario("falhas@escola.com");
        Instant bloqueadoAte = Instant.now().plus(15, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MICROS);

        for (int i = 1; i < LIMITE; i++) {
            usuarioRepository.registrarFalha(usuario.getId(), bloqueadoAte, LIMITE);
            Usuario atual = usuarioRepository.findById(usuario.getId()).orElseThrow();
            assertEquals(i, atual.getTentativasFalhas());
            assertNull(atual.getBloqueadoAte());
        }

        usuarioRepository.registrarFalha(usuario.getId(), bloqueadoAte, LIMITE);

        Usuario bloqueado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertEquals(LIMITE, bloqueado.getTentativasFalhas());
        assertEquals(bloqueadoAte, bloqueado.getBloqueadoAte());
    }

    @Test
    void zerarFalhasResetaContadorEDesbloqueia() {
        Usuario usuario = novoUsuario("reset@escola.com");
        Instant bloqueadoAte = Instant.now().plus(15, ChronoUnit.MINUTES);
        for (int i = 0; i < LIMITE; i++) {
            usuarioRepository.registrarFalha(usuario.getId(), bloqueadoAte, LIMITE);
        }

        usuarioRepository.zerarFalhas(usuario.getId());

        Usuario atual = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertEquals(0, atual.getTentativasFalhas());
        assertNull(atual.getBloqueadoAte());
    }
}
