package com.missio.fluencia_leitora.historicoevolucao;

import com.missio.fluencia_leitora.avaliacao.Avaliacao;
import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
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
 * HIST-01..06, HIST-22: histórico de avaliações finalizadas de um aluno
 * (design.md, Components). Rota aninhada sob {@code /alunos/{alunoId}},
 * mesmo padrão de {@code MatriculaController} (design.md, Tech Decisions).
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
}
