package com.missio.fluencia_leitora.regrasclassificacao;

import com.missio.fluencia_leitora.regrasclassificacao.dto.RegraClassificacaoResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
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
}
