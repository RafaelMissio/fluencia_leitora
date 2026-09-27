package com.missio.fluencia_leitora.common.security;

/**
 * Abstrai "quem está chamando a API": o perfil do usuário autenticado e,
 * quando o perfil é PROFESSOR, o seu {@code professorId}.
 */
public interface ContextoUsuarioPort {

    Perfil perfilAtual();

    Long professorIdAtual();
}
