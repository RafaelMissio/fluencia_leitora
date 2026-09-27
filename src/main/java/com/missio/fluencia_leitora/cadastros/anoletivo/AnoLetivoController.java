package com.missio.fluencia_leitora.cadastros.anoletivo;

import com.missio.fluencia_leitora.cadastros.anoletivo.dto.AnoLetivoResponse;
import com.missio.fluencia_leitora.cadastros.anoletivo.dto.AtualizarConfiguracaoRequest;
import com.missio.fluencia_leitora.cadastros.anoletivo.dto.ConfiguracaoAvaliacaoResponse;
import com.missio.fluencia_leitora.cadastros.anoletivo.dto.CriarAnoLetivoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CAD-01/CAD-02/CAD-03/CAD-04/CAD-06/CAD-19/CAD-20: CRUD de ano letivo,
 * ativação e configuração de palavras por série.
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnoLetivoResponse criar(@Valid @RequestBody CriarAnoLetivoRequest request) {
        AnoLetivo criado = anoLetivoService.criar(request.ano(), request.dataInicio(), request.dataFim());
        return AnoLetivoResponse.from(criado);
    }

    @PostMapping("/{id}/ativar")
    public AnoLetivoResponse ativar(@PathVariable Long id) {
        return AnoLetivoResponse.from(anoLetivoService.ativar(id));
    }

    @PutMapping("/{id}/configuracoes/{serie}")
    public ConfiguracaoAvaliacaoResponse atualizarConfiguracao(
            @PathVariable Long id, @PathVariable int serie, @RequestBody AtualizarConfiguracaoRequest request) {
        ConfiguracaoAvaliacao atualizada = configuracaoAvaliacaoService.atualizar(
                id, serie, request.quantidadeMinima(), request.quantidadeMaxima());
        return ConfiguracaoAvaliacaoResponse.from(atualizada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        anoLetivoService.inativar(id);
    }
}
