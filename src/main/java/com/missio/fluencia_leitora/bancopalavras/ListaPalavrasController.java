package com.missio.fluencia_leitora.bancopalavras;

import com.missio.fluencia_leitora.bancopalavras.dto.AtualizarListaPalavrasRequest;
import com.missio.fluencia_leitora.bancopalavras.dto.CriarListaPalavrasRequest;
import com.missio.fluencia_leitora.bancopalavras.dto.ListaPalavrasResponse;
import com.missio.fluencia_leitora.bancopalavras.dto.ListaPalavrasResumoResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDENADOR')")
    public ListaPalavrasResponse atualizar(
            @PathVariable Long id, @Valid @RequestBody AtualizarListaPalavrasRequest request) {
        return ListaPalavrasResponse.from(listaPalavrasService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDENADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        listaPalavrasService.inativar(id);
    }

    @GetMapping
    public List<ListaPalavrasResumoResponse> buscar(
            @RequestParam int serie, @RequestParam TipoLeituraCodigo tipoLeitura) {
        return listaPalavrasService.buscar(serie, tipoLeitura).stream()
                .map(ListaPalavrasResumoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ListaPalavrasResponse buscarPorId(@PathVariable Long id) {
        return ListaPalavrasResponse.from(listaPalavrasService.buscarPorId(id));
    }
}
