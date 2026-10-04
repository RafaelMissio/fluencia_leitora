package com.missio.fluencia_leitora.cadastros.anoletivo;

import com.missio.fluencia_leitora.cadastros.anoletivo.dto.AnoLetivoResponse;
import com.missio.fluencia_leitora.cadastros.anoletivo.dto.AtualizarConfiguracaoRequest;
import com.missio.fluencia_leitora.cadastros.anoletivo.dto.ConfiguracaoAvaliacaoResponse;
import com.missio.fluencia_leitora.cadastros.anoletivo.dto.CriarAnoLetivoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CAD-01/CAD-02/CAD-03/CAD-04/CAD-06/CAD-19/CAD-20: CRUD de ano letivo,
 * ativação e configuração de palavras por série. Escrita restrita ao
 * COORDENADOR (AUTH-07). CAD-21: leitura da configuração do ano ATIVO,
 * aberta a PROFESSOR+COORDENADOR - adicionada para `frontend-web` (ver
 * javadoc de {@link ConfiguracaoAvaliacaoService#buscarAtivaPorSerie}).
 */
@RestController
@RequestMapping("/api/v1/anos-letivos")
public class AnoLetivoController {

    private final AnoLetivoService anoLetivoService;
    private final ConfiguracaoAvaliacaoService configuracaoAvaliacaoService;

    public AnoLetivoController(
            AnoLetivoService anoLetivoService, ConfiguracaoAvaliacaoService configuracaoAvaliacaoService) {
        this.anoLetivoService = anoLetivoService;
        this.configuracaoAvaliacaoService = configuracaoAvaliacaoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDENADOR')")
    public List<AnoLetivoResponse> listar() {
        return anoLetivoService.listar().stream().map(AnoLetivoResponse::from).toList();
    }

    @GetMapping("/{id}/configuracoes")
    @PreAuthorize("hasRole('COORDENADOR')")
    public List<ConfiguracaoAvaliacaoResponse> listarConfiguracoes(@PathVariable Long id) {
        return configuracaoAvaliacaoService.listarPorAnoLetivo(id).stream()
                .map(ConfiguracaoAvaliacaoResponse::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDENADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public AnoLetivoResponse criar(@Valid @RequestBody CriarAnoLetivoRequest request) {
        AnoLetivo criado = anoLetivoService.criar(request.ano(), request.dataInicio(), request.dataFim());
        return AnoLetivoResponse.from(criado);
    }

    @PostMapping("/{id}/ativar")
    @PreAuthorize("hasRole('COORDENADOR')")
    public AnoLetivoResponse ativar(@PathVariable Long id) {
        return AnoLetivoResponse.from(anoLetivoService.ativar(id));
    }

    @PutMapping("/{id}/configuracoes/{serie}")
    @PreAuthorize("hasRole('COORDENADOR')")
    public ConfiguracaoAvaliacaoResponse atualizarConfiguracao(
            @PathVariable Long id, @PathVariable int serie, @RequestBody AtualizarConfiguracaoRequest request) {
        ConfiguracaoAvaliacao atualizada = configuracaoAvaliacaoService.atualizar(
                id, serie, request.quantidadeMinima(), request.quantidadeMaxima());
        return ConfiguracaoAvaliacaoResponse.from(atualizada);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDENADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        anoLetivoService.inativar(id);
    }

    @GetMapping("/ativo/configuracoes/{serie}")
    @PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR')")
    public ConfiguracaoAvaliacaoResponse buscarConfiguracaoAtiva(@PathVariable int serie) {
        return ConfiguracaoAvaliacaoResponse.from(configuracaoAvaliacaoService.buscarAtivaPorSerie(serie));
    }
}
