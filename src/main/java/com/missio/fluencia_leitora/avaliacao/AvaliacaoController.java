package com.missio.fluencia_leitora.avaliacao;

import com.missio.fluencia_leitora.avaliacao.dto.AvaliacaoResponse;
import com.missio.fluencia_leitora.avaliacao.dto.NovaAvaliacaoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * AVA-01..AVA-08: criação da avaliação; AVA-09..AVA-14: transições de
 * status. As ações são restritas ao PROFESSOR (SDD §17). O pertencimento
 * (AUTH-09) é verificado no service, que é quem conhece a matrícula e a
 * avaliação.
 */
@RestController
@RequestMapping("/api/v1/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PROFESSOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public AvaliacaoResponse criar(@Valid @RequestBody NovaAvaliacaoRequest request) {
        return AvaliacaoResponse.from(avaliacaoService.criar(request));
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasRole('PROFESSOR')")
    public AvaliacaoResponse iniciar(@PathVariable Long id) {
        return AvaliacaoResponse.from(avaliacaoService.iniciar(id));
    }

    @PostMapping("/{id}/pausar")
    @PreAuthorize("hasRole('PROFESSOR')")
    public AvaliacaoResponse pausar(@PathVariable Long id) {
        return AvaliacaoResponse.from(avaliacaoService.pausar(id));
    }

    @PostMapping("/{id}/continuar")
    @PreAuthorize("hasRole('PROFESSOR')")
    public AvaliacaoResponse continuar(@PathVariable Long id) {
        return AvaliacaoResponse.from(avaliacaoService.continuar(id));
    }

    @PostMapping("/{id}/resetar")
    @PreAuthorize("hasRole('PROFESSOR')")
    public AvaliacaoResponse resetar(@PathVariable Long id) {
        return AvaliacaoResponse.from(avaliacaoService.resetar(id));
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasRole('PROFESSOR')")
    public AvaliacaoResponse finalizar(@PathVariable Long id) {
        return AvaliacaoResponse.from(avaliacaoService.finalizar(id));
    }
}
