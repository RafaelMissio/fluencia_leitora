package com.missio.fluencia_leitora.historicoevolucao;

import com.missio.fluencia_leitora.avaliacao.Avaliacao;
import com.missio.fluencia_leitora.avaliacao.AvaliacaoAudioRepository;
import com.missio.fluencia_leitora.avaliacao.AvaliacaoRepository;
import com.missio.fluencia_leitora.avaliacao.StatusAvaliacao;
import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.cadastros.aluno.AlunoService;
import com.missio.fluencia_leitora.cadastros.aluno.Matricula;
import com.missio.fluencia_leitora.common.security.PertencimentoProfessorGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * HIST-01..06: histórico de avaliações finalizadas de um aluno (design.md,
 * Components). {@code historico}/{@code comAudio} reaproveitam a mesma
 * checagem de ownership de {@code AlunoController.buscarPorId} (AUTH-09),
 * mas aqui dentro do service em vez do controller, porque a feature expõe
 * 3 endpoints que compartilham a mesma resolução de aluno.
 */
@Service
public class HistoricoEvolucaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final AvaliacaoAudioRepository avaliacaoAudioRepository;
    private final AlunoService alunoService;
    private final PertencimentoProfessorGuard pertencimentoProfessorGuard;

    public HistoricoEvolucaoService(
            AvaliacaoRepository avaliacaoRepository,
            AvaliacaoAudioRepository avaliacaoAudioRepository,
            AlunoService alunoService,
            PertencimentoProfessorGuard pertencimentoProfessorGuard) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.avaliacaoAudioRepository = avaliacaoAudioRepository;
        this.alunoService = alunoService;
        this.pertencimentoProfessorGuard = pertencimentoProfessorGuard;
    }

    @Transactional(readOnly = true)
    public Page<Avaliacao> historico(
            Long alunoId, Long anoLetivoId, TipoLeituraCodigo tipoLeitura, Long cicloId, Pageable pageable) {
        AlunoService.AlunoBusca alunoBusca = alunoService.buscarPorId(alunoId);
        verificarPertencimento(alunoBusca.matriculaAtiva());

        return avaliacaoRepository.buscarHistorico(
                alunoId, StatusAvaliacao.FINALIZADA, anoLetivoId, tipoLeitura, cicloId, pageable);
    }

    @Transactional(readOnly = true)
    public Set<Long> comAudio(List<Long> avaliacaoIds) {
        return new HashSet<>(avaliacaoAudioRepository.findAvaliacaoIdByAvaliacaoIdIn(avaliacaoIds));
    }

    private void verificarPertencimento(Matricula matriculaAtiva) {
        pertencimentoProfessorGuard.verificar(
                matriculaAtiva == null || matriculaAtiva.getProfessor() == null
                        ? null
                        : matriculaAtiva.getProfessor().getId());
    }
}
