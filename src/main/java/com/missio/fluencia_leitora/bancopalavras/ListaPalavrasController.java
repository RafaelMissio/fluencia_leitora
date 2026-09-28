package com.missio.fluencia_leitora.bancopalavras;

import com.missio.fluencia_leitora.bancopalavras.dto.CriarListaPalavrasRequest;
import com.missio.fluencia_leitora.bancopalavras.dto.ListaPalavrasResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * PAL-01..PAL-12: CRUD de listas de palavras/pseudopalavras/textos curtos.
 * Escrita restrita ao COORDENADOR; leitura aberta a qualquer autenticado
 * (design.md, Architecture Overview).
 */
@RestController
@RequestMapping("/api/v1/listas-palavras")
public class ListaPalavrasController {

    private final ListaPalavrasService listaPalavrasService;

    public ListaPalavrasController(ListaPalavrasService listaPalavrasService) {
        this.listaPalavrasService = listaPalavrasService;
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDENADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ListaPalavrasResponse criar(@Valid @RequestBody CriarListaPalavrasRequest request) {
        return ListaPalavrasResponse.from(listaPalavrasService.criar(request));
    }
}
