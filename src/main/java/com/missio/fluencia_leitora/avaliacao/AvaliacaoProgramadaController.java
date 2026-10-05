package com.missio.fluencia_leitora.avaliacao;

import com.missio.fluencia_leitora.avaliacao.dto.AplicarAvaliacaoProgramadaRequest;
import com.missio.fluencia_leitora.avaliacao.dto.AvaliacaoProgramadaResponse;
import com.missio.fluencia_leitora.avaliacao.dto.AvaliacaoResponse;
import com.missio.fluencia_leitora.avaliacao.dto.NovaAvaliacaoProgramadaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Avaliações configuradas por série: o COORDENADOR cadastra; o PROFESSOR aplica ao aluno da série. */
@RestController
@RequestMapping("/api/v1/avaliacoes-programadas")
public class AvaliacaoProgramadaController {

    private final AvaliacaoProgramadaService service;
    private final AvaliacaoService avaliacaoService;

    public AvaliacaoProgramadaController(AvaliacaoProgramadaService service, AvaliacaoService avaliacaoService) {
        this.service = service;
        this.avaliacaoService = avaliacaoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDENADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public AvaliacaoProgramadaResponse criar(@Valid @RequestBody NovaAvaliacaoProgramadaRequest request) {
        return AvaliacaoProgramadaResponse.from(service.criar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR')")
    public List<AvaliacaoProgramadaResponse> listar(
            @RequestParam(required = false) Integer serie, @RequestParam(required = false) Long anoLetivoId) {
        return service.listar(serie, anoLetivoId).stream().map(AvaliacaoProgramadaResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDENADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable Long id) {
        service.desativar(id);
    }

    @PostMapping("/{id}/aplicar")
    @PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR')")
    public AvaliacaoResponse aplicar(@PathVariable Long id, @Valid @RequestBody AplicarAvaliacaoProgramadaRequest request) {
        return AvaliacaoResponse.from(avaliacaoService.aplicarProgramada(id, request.alunoId()));
    }
}
