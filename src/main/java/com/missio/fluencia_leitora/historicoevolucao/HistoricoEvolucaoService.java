package com.missio.fluencia_leitora.historicoevolucao;

import com.missio.fluencia_leitora.avaliacao.Avaliacao;
import com.missio.fluencia_leitora.avaliacao.AvaliacaoAudioRepository;
import com.missio.fluencia_leitora.avaliacao.AvaliacaoRepository;
import com.missio.fluencia_leitora.avaliacao.StatusAvaliacao;
import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.cadastros.aluno.AlunoService;
import com.missio.fluencia_leitora.cadastros.aluno.Matricula;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.security.PertencimentoProfessorGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * HIST-01..11, HIST-20..21: histórico e evolução por ciclo de avaliações
 * finalizadas de um aluno (design.md, Components). {@code historico}/{@code
 * comAudio} reaproveitam a mesma checagem de ownership de {@code
 * AlunoController.buscarPorId} (AUTH-09), mas aqui dentro do service em vez
 * do controller, porque a feature expõe 3 endpoints que compartilham a
 * mesma resolução de aluno. {@code evolucaoPorCiclo} não faz essa checagem
 * (design.md, Tech Decisions - RF013 é restrito só por papel).
 */
@Service
public class HistoricoEvolucaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final AvaliacaoAudioRepository avaliacaoAudioRepository;
    private final AlunoService alunoService;
    private final AnoLetivoRepository anoLetivoRepository;
    private final PertencimentoProfessorGuard pertencimentoProfessorGuard;

    public HistoricoEvolucaoService(
            AvaliacaoRepository avaliacaoRepository,
            AvaliacaoAudioRepository avaliacaoAudioRepository,
            AlunoService alunoService,
            AnoLetivoRepository anoLetivoRepository,
            PertencimentoProfessorGuard pertencimentoProfessorGuard) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.avaliacaoAudioRepository = avaliacaoAudioRepository;
        this.alunoService = alunoService;
        this.anoLetivoRepository = anoLetivoRepository;
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

    /**
     * HIST-07..11: os três ciclos (ENTRADA/ACOMPANHAMENTO/SAIDA) do ano
     * informado (ou o ano ATIVO), cada um com a avaliação FINALIZADA mais
     * recente (HIST-20) ou {@code null} quando o ciclo não tem avaliação.
     *
     * <p>Sem {@link PertencimentoProfessorGuard} de propósito: design.md,
     * Tech Decisions - RF013 é tratado como relatório gerencial, restrito só
     * por papel (`hasRole('COORDENADOR')` no controller), sem checar aluno
     * específico.
     */
    @Transactional(readOnly = true)
    public EvolucaoCiclos evolucaoPorCiclo(Long alunoId, Long anoLetivoId, TipoLeituraCodigo tipoLeitura) {
        alunoService.buscarPorId(alunoId);
        AnoLetivo anoLetivo = resolverAnoLetivo(anoLetivoId);

        List<Avaliacao> finalizadas = avaliacaoRepository.buscarFinalizadasPorAnoETipo(
                alunoId, StatusAvaliacao.FINALIZADA, anoLetivo.getId(), tipoLeitura);
        Collection<Avaliacao> maisRecentePorCiclo =
                maisRecentePorGrupo(finalizadas, avaliacao -> avaliacao.getCiclo().getId()).values();

        return new EvolucaoCiclos(
                alunoId,
                anoLetivo.getAno(),
                tipoLeitura,
                porCicloCodigo(maisRecentePorCiclo, "ENTRADA"),
                porCicloCodigo(maisRecentePorCiclo, "ACOMPANHAMENTO"),
                porCicloCodigo(maisRecentePorCiclo, "SAIDA"));
    }

    private AnoLetivo resolverAnoLetivo(Long anoLetivoId) {
        if (anoLetivoId != null) {
            return anoLetivoRepository.findById(anoLetivoId).orElseThrow(this::anoLetivoNaoEncontrado);
        }
        return anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO).stream()
                .findFirst()
                .orElseThrow(this::anoLetivoNaoEncontrado);
    }

    private BusinessException anoLetivoNaoEncontrado() {
        return new BusinessException(HttpStatus.NOT_FOUND, "ANO_LETIVO_NAO_ENCONTRADO", "Ano letivo não encontrado");
    }

    /**
     * HIST-20: para cada valor de {@code chave}, mantém só a 1ª ocorrência -
     * a lista de entrada já vem ordenada com a mais recente primeiro dentro
     * de cada grupo (design.md, Tech Decisions - agrupamento em Java, não SQL).
     */
    private static <T> Map<Long, T> maisRecentePorGrupo(List<T> ordenadosPorGrupo, Function<T, Long> chave) {
        Map<Long, T> maisRecentePorChave = new LinkedHashMap<>();
        for (T item : ordenadosPorGrupo) {
            maisRecentePorChave.putIfAbsent(chave.apply(item), item);
        }
        return maisRecentePorChave;
    }

    private static Avaliacao porCicloCodigo(Collection<Avaliacao> avaliacoes, String codigoCiclo) {
        return avaliacoes.stream()
                .filter(avaliacao -> avaliacao.getCiclo().getCodigo().equals(codigoCiclo))
                .findFirst()
                .orElse(null);
    }

    private void verificarPertencimento(Matricula matriculaAtiva) {
        pertencimentoProfessorGuard.verificar(
                matriculaAtiva == null || matriculaAtiva.getProfessor() == null
                        ? null
                        : matriculaAtiva.getProfessor().getId());
    }

    /** HIST-07..11: tipo de retorno interno, mapeado a DTO no controller (T7). */
    public record EvolucaoCiclos(
            Long alunoId,
            int anoLetivo,
            TipoLeituraCodigo tipoLeitura,
            Avaliacao entrada,
            Avaliacao acompanhamento,
            Avaliacao saida) {
    }
}
