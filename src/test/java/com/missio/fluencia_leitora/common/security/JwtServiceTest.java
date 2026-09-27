package com.missio.fluencia_leitora.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AUTH-01/AUTH-06 + edge cases "JWT com assinatura adulterada" e
 * "APP_JWT_SECRET com menos de 32 bytes": emissão HS256 com validade de 8h,
 * validação que devolve vazio (sem lançar) para token adulterado, expirado ou
 * malformado, e falha de inicialização com segredo curto.
 */
class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes-ok";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SEGREDO);
        jwtService.validarSegredo();
    }

    private static SecretKey chave(String segredo) {
        return Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void emitirProduzTokenQueValidaDeVoltaParaOMesmoUsuarioIdComValidadeDe8Horas() {
        String token = jwtService.emitir(42L);

        assertEquals(Optional.of(42L), jwtService.validarERetornarUsuarioId(token));
        Claims claims = Jwts.parser().verifyWith(chave(SEGREDO)).build().parseSignedClaims(token).getPayload();
        assertEquals("42", claims.getSubject());
        long validadeSegundos = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;
        assertEquals(28800L, validadeSegundos);
    }

    @Test
    void tokenComAssinaturaAdulteradaRetornaVazio() {
        String original = jwtService.emitir(1L);
        String forjado = Jwts.builder()
                .subject("2")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(chave("outro-segredo-qualquer-com-mais-de-32-bytes"))
                .compact();
        String[] partesOriginal = original.split("\\.");
        String[] partesForjado = forjado.split("\\.");
        // payload trocado (sub=2) mantendo a assinatura do token original
        String adulterado = partesOriginal[0] + "." + partesForjado[1] + "." + partesOriginal[2];

        assertEquals(Optional.empty(), jwtService.validarERetornarUsuarioId(adulterado));
    }

    @Test
    void tokenExpiradoRetornaVazio() {
        String expirado = Jwts.builder()
                .subject("7")
                .issuedAt(Date.from(Instant.now().minusSeconds(28800 + 60)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(chave(SEGREDO))
                .compact();

        assertEquals(Optional.empty(), jwtService.validarERetornarUsuarioId(expirado));
    }

    @Test
    void tokenMalformadoRetornaVazio() {
        assertEquals(Optional.empty(), jwtService.validarERetornarUsuarioId("isto-nao-e-um-jwt"));
        assertEquals(Optional.empty(), jwtService.validarERetornarUsuarioId(""));
    }

    @Test
    void segredoComMenosDe32BytesFazOContextoFalharAoSubir() {
        new ApplicationContextRunner()
                .withUserConfiguration(JwtService.class)
                .withPropertyValues("APP_JWT_SECRET=curto-demais-31-bytes-xxxxxxxxx")
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    Throwable causa = context.getStartupFailure();
                    while (causa.getCause() != null) {
                        causa = causa.getCause();
                    }
                    assertInstanceOf(IllegalStateException.class, causa);
                    assertTrue(causa.getMessage().contains("APP_JWT_SECRET"));
                });
    }
}
