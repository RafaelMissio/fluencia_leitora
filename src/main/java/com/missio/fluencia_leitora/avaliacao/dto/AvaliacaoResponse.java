package com.missio.fluencia_leitora.avaliacao.dto;

import com.missio.fluencia_leitora.avaliacao.Avaliacao;
import com.missio.fluencia_leitora.avaliacao.StatusAvaliacao;
import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.regrasclassificacao.Fase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * AVA-01/AVA-23: configuração, status, cópias da matrícula e palavras. Os
 * campos de resultado e classificação (SDD §13) só vêm preenchidos quando a
 * avaliação está {@code FINALIZADA}; fora disso são {@code null}.
 * {@code classificacaoPendente} é {@code true} quando a avaliação foi
 * finalizada sem faixa de classificação (AVA-22).
 */
public record AvaliacaoResponse(
        Long id,
        Long alunoId,
        Long professorId,
        String professorNome,
        Long turmaId,
        String turmaNome,
        int serie,
        Long anoLetivoId,
        Long cicloId,
        TipoLeituraCodigo tipoLeitura,
        LocalDate dataAvaliacao,
        int tempoConfiguradoSegundos,
        StatusAvaliacao status,
        Instant iniciadoEm,
        Instant finalizadoEm,
        int quantidadeTotal,
        Integer tempoUtilizadoSegundos,
        Integer quantidadeCorretas,
        Integer quantidadeIncorretas,
        Integer quantidadeNaoLidas,
        Integer quantidadeLidas,
        BigDecimal percentualAcerto,
        Fase fase,
        Integer nivel,
        boolean classificacaoPendente,
        boolean ativa,
        Integer refeitas,
        int maxRefazeres,
        Boolean podeRefazer,
        List<PalavraAvaliacaoResponse> palavras) {

    /** Sem a contagem de refeitas (ações que não a consultam): {@code refeitas} e {@code podeRefazer} vêm {@code null}. */
    public static AvaliacaoResponse from(Avaliacao avaliacao) {
        return from(avaliacao, null);
    }

    public static AvaliacaoResponse from(Avaliacao avaliacao, Integer refeitas) {
        boolean finalizada = avaliacao.getStatus() == StatusAvaliacao.FINALIZADA;
        List<PalavraAvaliacaoResponse> palavras =
                avaliacao.getPalavras().stream().map(PalavraAvaliacaoResponse::from).toList();

        return new AvaliacaoResponse(
                avaliacao.getId(),
                avaliacao.getAluno().getId(),
                avaliacao.getProfessor() == null ? null : avaliacao.getProfessor().getId(),
                avaliacao.getProfessorNome(),
                avaliacao.getTurma().getId(),
                avaliacao.getTurmaNome(),
                avaliacao.getSerie(),
                avaliacao.getAnoLetivo().getId(),
                avaliacao.getCiclo().getId(),
                avaliacao.getTipoLeitura(),
                avaliacao.getDataAvaliacao(),
                avaliacao.getTempoConfiguradoSegundos(),
                avaliacao.getStatus(),
                avaliacao.getIniciadoEm(),
                avaliacao.getFinalizadoEm(),
                avaliacao.getQuantidadeTotal(),
                finalizada ? avaliacao.getTempoUtilizadoSegundos() : null,
                finalizada ? avaliacao.getQuantidadeCorretas() : null,
                finalizada ? avaliacao.getQuantidadeIncorretas() : null,
                finalizada ? avaliacao.getQuantidadeNaoLidas() : null,
                finalizada ? avaliacao.getQuantidadeCorretas() + avaliacao.getQuantidadeIncorretas() : null,
                finalizada ? avaliacao.getPercentualAcerto() : null,
                finalizada ? avaliacao.getFase() : null,
                finalizada ? avaliacao.getNivel() : null,
                finalizada && avaliacao.getFase() == null,
                avaliacao.isAtiva(),
                refeitas,
                avaliacao.getMaxRefazeres(),
                refeitas == null ? null : finalizada && avaliacao.isAtiva() && refeitas < avaliacao.getMaxRefazeres(),
                palavras);
    }
}
