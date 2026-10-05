package com.missio.fluencia_leitora.avaliacao.dto;

import com.missio.fluencia_leitora.avaliacao.StatusAvaliacao;
import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;

/**
 * Uma avaliação que o aluno ainda precisa fazer. {@code avaliacaoId} e
 * {@code status} são {@code null} quando ela veio da configuração da série e
 * ainda não foi iniciada para este aluno ({@code programadaId} preenchido);
 * {@code programadaId} é {@code null} nas avaliações criadas direto pelo professor
 * (que não têm {@code nome}).
 */
public record AvaliacaoPendenteResponse(
        Long programadaId,
        Long avaliacaoId,
        String nome,
        TipoLeituraCodigo tipoLeitura,
        Long cicloId,
        int tempoSegundos,
        StatusAvaliacao status) {
}
