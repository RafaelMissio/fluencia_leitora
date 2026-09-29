package com.missio.fluencia_leitora.historicoevolucao;

import com.missio.fluencia_leitora.avaliacao.Avaliacao;
import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.historicoevolucao.HistoricoEvolucaoService.EvolucaoCiclos;
import com.missio.fluencia_leitora.historicoevolucao.dto.EvolucaoCiclosResponse;
import com.missio.fluencia_leitora.historicoevolucao.dto.HistoricoAvaliacaoItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * HIST-01..11, HIST-21..22: histórico e evolução por ciclo de avaliações
 * finalizadas de um aluno (design.md, Components). Rota aninhada sob
 * {@code /alunos/{alunoId}}, mesmo padrão de {@code MatriculaController}
 * (design.md, Tech Decisions).
 */
@RestController
@RequestMapping("/api/v1/alunos/{alunoId}")
public class HistoricoEvolucaoController {

    private static final int TAMANHO_PAGINA = 20;

    private final HistoricoEvolucaoService historicoEvolucaoService;

    public HistoricoEvolucaoController(HistoricoEvolucaoService historicoEvolucaoService) {
        this.historicoEvolucaoService = historicoEvolucaoService;
    }

    @GetMapping("/historico-avaliacoes")
    @PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR')")
    public Page<HistoricoAvaliacaoItemResponse> historico(
            @PathVariable Long alunoId,
            @RequestParam(required = false) Long anoLetivoId,
            @RequestParam(required = false) TipoLeituraCodigo tipoLeitura,
            @RequestParam(required = false) Long cicloId,
            @RequestParam(defaultValue = "0") int page) {
        Page<Avaliacao> pagina = historicoEvolucaoService.historico(
                alunoId, anoLetivoId, tipoLeitura, cicloId, PageRequest.of(page, TAMANHO_PAGINA));

        List<Long> avaliacaoIds = pagina.getContent().stream().map(Avaliacao::getId).toList();
        Set<Long> comAudio = historicoEvolucaoService.comAudio(avaliacaoIds);

        return pagina.map(avaliacao -> HistoricoAvaliacaoItemResponse.from(avaliacao, comAudio.contains(avaliacao.getId())));
    }

    /**
     * HIST-07..11, HIST-21: {@code tipoLeitura} obrigatório (sem {@code
     * defaultValue}/{@code required = false}) - a ausência já vira 400 via
     * {@code MissingServletRequestParameterException}, tratada pelo
     * {@code ResponseEntityExceptionHandler} herdado (design.md, Error
     * Handling Strategy).
     */
    @GetMapping("/evolucao-ciclos")
    @PreAuthorize("hasRole('COORDENADOR')")
    public EvolucaoCiclosResponse evolucaoCiclos(
            @PathVariable Long alunoId,
            @RequestParam(required = false) Long anoLetivoId,
            @RequestParam TipoLeituraCodigo tipoLeitura) {
        EvolucaoCiclos evolucaoCiclos = historicoEvolucaoService.evolucaoPorCiclo(alunoId, anoLetivoId, tipoLeitura);
        return EvolucaoCiclosResponse.from(evolucaoCiclos);
    }
}
