package com.missio.fluencia_leitora.regrasclassificacao;

import com.missio.fluencia_leitora.regrasclassificacao.dto.HistoricoVersaoResponse;
import com.missio.fluencia_leitora.regrasclassificacao.dto.RegraClassificacaoResponse;
import com.missio.fluencia_leitora.regrasclassificacao.dto.SubstituirRegrasClassificacaoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REG-06: leitura das faixas ativas de uma série. Aberta a qualquer perfil
 * autenticado (design.md, Components - `RegraClassificacaoController`).
 */
@RestController
@RequestMapping("/api/v1/regras-classificacao")
public class RegraClassificacaoController {

    private final RegraClassificacaoService service;

    public RegraClassificacaoController(RegraClassificacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<RegraClassificacaoResponse> buscarAtivas(@RequestParam @Min(1) @Max(5) int serie) {
        return service.buscarAtivas(serie).stream()
                .map(RegraClassificacaoResponse::from)
                .toList();
    }

    @PutMapping("/series/{serie}")
    @PreAuthorize("hasRole('COORDENADOR')")
    public List<RegraClassificacaoResponse> substituir(
            @PathVariable @Min(1) @Max(5) int serie,
            @Valid @RequestBody SubstituirRegrasClassificacaoRequest request) {
        return service.substituir(serie, request.faixas()).stream()
                .map(RegraClassificacaoResponse::from)
                .toList();
    }

    @GetMapping("/historico")
    public List<HistoricoVersaoResponse> historico(@RequestParam @Min(1) @Max(5) int serie) {
        return service.buscarHistorico(serie).stream()
                .map(grupo -> new HistoricoVersaoResponse(
                        grupo.get(0).getAlteradoEm(),
                        grupo.get(0).getAlteradoPor(),
                        grupo.stream().map(RegraClassificacaoResponse::from).toList()))
                .toList();
    }
}
