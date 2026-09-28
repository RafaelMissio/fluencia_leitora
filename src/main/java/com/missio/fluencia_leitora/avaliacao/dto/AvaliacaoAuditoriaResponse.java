package com.missio.fluencia_leitora.avaliacao.dto;

import com.missio.fluencia_leitora.avaliacao.AcaoAuditoria;
import com.missio.fluencia_leitora.avaliacao.AvaliacaoAuditoria;

import java.time.Instant;

/**
 * AVA-26: um registro de auditoria. {@code usuario} é o id do usuário que
 * fez a alteração (a entidade guarda só {@code usuarioId}).
 */
public record AvaliacaoAuditoriaResponse(
        Long usuario,
        Instant dataHora,
        AcaoAuditoria acao,
        String valorAnterior,
        String valorNovo,
        String justificativa) {

    public static AvaliacaoAuditoriaResponse from(AvaliacaoAuditoria auditoria) {
        return new AvaliacaoAuditoriaResponse(
                auditoria.getUsuarioId(),
                auditoria.getDataHora(),
                auditoria.getAcao(),
                auditoria.getValorAnterior(),
                auditoria.getValorNovo(),
                auditoria.getJustificativa());
    }
}
