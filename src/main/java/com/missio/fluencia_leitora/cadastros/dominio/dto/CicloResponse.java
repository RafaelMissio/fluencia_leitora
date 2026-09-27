package com.missio.fluencia_leitora.cadastros.dominio.dto;

import com.missio.fluencia_leitora.cadastros.dominio.Ciclo;

public record CicloResponse(Long id, String codigo, String descricao) {

    public static CicloResponse from(Ciclo ciclo) {
        return new CicloResponse(ciclo.getId(), ciclo.getCodigo(), ciclo.getDescricao());
    }
}
