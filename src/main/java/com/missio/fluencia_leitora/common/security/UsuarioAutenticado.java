package com.missio.fluencia_leitora.common.security;

/** Principal do {@code SecurityContext}: dados lidos do banco a cada requisição. */
public record UsuarioAutenticado(Long usuarioId, Perfil perfil, Long professorId) {
}
