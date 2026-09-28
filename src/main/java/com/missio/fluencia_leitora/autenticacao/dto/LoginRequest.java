package com.missio.fluencia_leitora.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;

/** AUTH-01: credenciais enviadas ao login. */
public record LoginRequest(@NotBlank String email, @NotBlank String senha) {

    /** Nunca expõe a senha em logs ou mensagens de erro (AUTH-05). */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
