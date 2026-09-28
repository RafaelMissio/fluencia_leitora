package com.missio.fluencia_leitora.regrasclassificacao.dto;

import java.time.Instant;
import java.util.List;

/**
 * Um grupo do histórico de faixas de uma série (P2, REG-15). {@code
 * alteradoEm=null} identifica o grupo corrente (ainda não substituído).
 */
public record HistoricoVersaoResponse(Instant alteradoEm, Long alteradoPor, List<RegraClassificacaoResponse> faixas) {
}
