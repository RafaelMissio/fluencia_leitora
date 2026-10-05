package com.missio.fluencia_leitora.autenticacao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints públicos de recuperação de senha. */
@RestController
@RequestMapping("/api/v1/auth")
public class RecuperacaoSenhaController {

    private final RecuperacaoSenhaService service;

    public RecuperacaoSenhaController(RecuperacaoSenhaService service) {
        this.service = service;
    }

    /** Sempre 204, exista ou não o e-mail, para não vazar quais contas existem. */
    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> solicitar(@Valid @RequestBody SolicitarRequest request) {
        service.solicitar(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinir(@Valid @RequestBody RedefinirRequest request) {
        service.redefinir(request.token(), request.novaSenha());
        return ResponseEntity.noContent().build();
    }

    public record SolicitarRequest(@NotBlank String email) {
    }

    public record RedefinirRequest(@NotBlank String token, @NotBlank String novaSenha) {

        @Override
        public String toString() {
            return "RedefinirRequest[]";
        }
    }
}
