package com.missio.fluencia_leitora.cadastros.anoletivo.dto;

import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import jakarta.validation.constraints.NotNull;

/** Payload de alteração da situação do ano letivo (PLANEJADO, ATIVO, ENCERRADO). */
public record AlterarSituacaoAnoLetivoRequest(@NotNull SituacaoAnoLetivo situacao) {
}
