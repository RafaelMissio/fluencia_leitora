package com.missio.fluencia_leitora.cadastros.dominio.dto;

import com.missio.fluencia_leitora.cadastros.dominio.TipoLeitura;

public record TipoLeituraResponse(Long id, String codigo, String descricao) {

    public static TipoLeituraResponse from(TipoLeitura tipoLeitura) {
        return new TipoLeituraResponse(tipoLeitura.getId(), tipoLeitura.getCodigo(), tipoLeitura.getDescricao());
    }
}
