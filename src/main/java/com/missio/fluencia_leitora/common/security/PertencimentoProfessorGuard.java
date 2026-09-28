package com.missio.fluencia_leitora.common.security;

import com.missio.fluencia_leitora.common.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * AUTH-09: um PROFESSOR só acessa recursos cujo professor seja ele mesmo;
 * caso contrário responde 404, sem revelar que o recurso existe. O
 * COORDENADOR acessa tudo.
 */
@Component
public class PertencimentoProfessorGuard {

    private final ContextoUsuarioPort contextoUsuario;

    public PertencimentoProfessorGuard(ContextoUsuarioPort contextoUsuario) {
        this.contextoUsuario = contextoUsuario;
    }

    public void verificar(Long professorIdDoRecurso) {
        if (contextoUsuario.perfilAtual() == Perfil.PROFESSOR
                && !Objects.equals(professorIdDoRecurso, contextoUsuario.professorIdAtual())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", "Recurso não encontrado");
        }
    }
}
