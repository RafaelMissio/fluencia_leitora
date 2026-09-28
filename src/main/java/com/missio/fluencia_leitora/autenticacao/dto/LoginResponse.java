package com.missio.fluencia_leitora.autenticacao.dto;

import com.missio.fluencia_leitora.autenticacao.AuthService.LoginResult;
import com.missio.fluencia_leitora.common.security.Perfil;

/** AUTH-01: token + validade em segundos + perfil (+ professorId quando PROFESSOR). */
public record LoginResponse(String accessToken, long expiresIn, Perfil perfil, Long professorId) {

    public static LoginResponse from(LoginResult.Sucesso sucesso) {
        return new LoginResponse(sucesso.accessToken(), sucesso.expiresIn(), sucesso.perfil(), sucesso.professorId());
    }
}
