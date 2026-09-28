package com.missio.fluencia_leitora.autenticacao.dto;

import jakarta.validation.constraints.NotNull;

/** AUTH-11: nova senha definida pelo coordenador. A política de senha (AUTH-13) é validada no service. */
public record AlterarSenhaRequest(@NotNull String senha) {

    /** Nunca expõe a senha em logs ou mensagens de erro (AUTH-05). */
    @Override
    public String toString() {
        return "AlterarSenhaRequest[***]";
    }
}
