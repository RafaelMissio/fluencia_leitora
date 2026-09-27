package com.missio.fluencia_leitora.common.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Emite e valida JWT HS256 (AUTH-01, AUTH-06). O token carrega só o
 * {@code usuarioId} no claim {@code sub}; perfil e professor são relidos do
 * banco a cada requisição.
 */
@Service
public class JwtService {

    public static final Duration VALIDADE = Duration.ofHours(8);

    private static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;

    private final byte[] segredo;
    private SecretKey chave;

    public JwtService(@Value("${APP_JWT_SECRET:}") String segredo) {
        this.segredo = segredo.getBytes(StandardCharsets.UTF_8);
    }

    @PostConstruct
    void validarSegredo() {
        if (segredo.length < TAMANHO_MINIMO_SEGREDO_BYTES) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET deve ter pelo menos " + TAMANHO_MINIMO_SEGREDO_BYTES + " bytes");
        }
        this.chave = Keys.hmacShaKeyFor(segredo);
    }

    public String emitir(Long usuarioId) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(VALIDADE)))
                .signWith(chave)
                .compact();
    }

    /** Vazio quando o token tem assinatura inválida, está expirado ou malformado; nunca lança. */
    public Optional<Long> validarERetornarUsuarioId(String token) {
        try {
            String sub = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload().getSubject();
            return Optional.of(Long.valueOf(sub));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
