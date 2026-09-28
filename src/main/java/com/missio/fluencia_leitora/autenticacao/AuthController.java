package com.missio.fluencia_leitora.autenticacao;

import com.missio.fluencia_leitora.autenticacao.AuthService.LoginResult;
import com.missio.fluencia_leitora.autenticacao.dto.LoginRequest;
import com.missio.fluencia_leitora.autenticacao.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AUTH-01..AUTH-04: login público que devolve 200, 401 genérico ou 429 com Retry-After. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        LoginResult resultado = authService.login(request.email(), request.senha());
        return switch (resultado) {
            case LoginResult.Sucesso sucesso -> ResponseEntity.ok(LoginResponse.from(sucesso));
            case LoginResult.CredenciaisInvalidas invalidas -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(problema(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", invalidas.mensagem()));
            case LoginResult.Bloqueado bloqueado -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(bloqueado.segundosRestantes()))
                    .body(problema(HttpStatus.TOO_MANY_REQUESTS, "CONTA_BLOQUEADA",
                            "Conta bloqueada temporariamente por excesso de tentativas"));
        };
    }

    private static ProblemDetail problema(HttpStatus status, String code, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setProperty("code", code);
        return problemDetail;
    }
}
