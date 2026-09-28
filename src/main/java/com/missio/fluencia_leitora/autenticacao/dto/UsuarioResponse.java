package com.missio.fluencia_leitora.autenticacao.dto;

import com.missio.fluencia_leitora.autenticacao.UsuarioService.UsuarioResumo;
import com.missio.fluencia_leitora.common.security.Perfil;

/** AUTH-11: usuário devolvido pela API, sem senha nem hash. */
public record UsuarioResponse(Long id, String email, Perfil perfil, Long professorId, boolean ativo) {

    public static UsuarioResponse from(UsuarioResumo resumo) {
        return new UsuarioResponse(resumo.id(), resumo.email(), resumo.perfil(), resumo.professorId(), resumo.ativo());
    }
}
