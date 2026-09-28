package com.missio.fluencia_leitora.autenticacao.dto;

import com.missio.fluencia_leitora.common.security.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** AUTH-11: payload de criação de usuário. A política de senha (AUTH-13) é validada no service. */
public record CriarUsuarioRequest(
        @NotBlank @Email String email, @NotNull String senha, @NotNull Perfil perfil, Long professorId) {

    /** Nunca expõe a senha em logs ou mensagens de erro (AUTH-05). */
    @Override
    public String toString() {
        return "CriarUsuarioRequest[email=" + email + ", perfil=" + perfil + ", professorId=" + professorId + "]";
    }
}
