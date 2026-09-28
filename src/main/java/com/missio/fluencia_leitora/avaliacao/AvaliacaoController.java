package com.missio.fluencia_leitora.avaliacao;

import com.missio.fluencia_leitora.avaliacao.dto.AvaliacaoAuditoriaResponse;
import com.missio.fluencia_leitora.avaliacao.dto.AvaliacaoResponse;
import com.missio.fluencia_leitora.avaliacao.dto.CancelarAvaliacaoRequest;
import com.missio.fluencia_leitora.avaliacao.dto.MarcarPalavraRequest;
import com.missio.fluencia_leitora.avaliacao.dto.MarcarPalavrasRequest;
import com.missio.fluencia_leitora.avaliacao.dto.NovaAvaliacaoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * AVA-01..AVA-08: criação da avaliação; AVA-09..AVA-14: transições de
 * status; AVA-15, AVA-18, AVA-19: marcação das palavras; AVA-23, AVA-26:
 * consultas; AVA-24: cancelamento; AVA-27..AVA-32: envio e download do
 * áudio. As ações são restritas ao PROFESSOR (SDD §17); as consultas
 * também são abertas ao COORDENADOR. O pertencimento (AUTH-09) é
 * verificado no service, que é quem conhece a matrícula e a avaliação.
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

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('PROFESSOR')")
    public AvaliacaoResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelarAvaliacaoRequest request) {
        return AvaliacaoResponse.from(avaliacaoService.cancelar(id, request.justificativa()));
    }

    @PutMapping("/{id}/palavras/{ordem}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public AvaliacaoResponse marcarPalavra(
            @PathVariable Long id, @PathVariable int ordem, @Valid @RequestBody MarcarPalavraRequest request) {
        return AvaliacaoResponse.from(avaliacaoService.marcarPalavra(id, ordem, request.status()));
    }

    @PutMapping("/{id}/palavras")
    @PreAuthorize("hasRole('PROFESSOR')")
    public AvaliacaoResponse marcarPalavras(@PathVariable Long id, @Valid @RequestBody MarcarPalavrasRequest request) {
        return AvaliacaoResponse.from(avaliacaoService.marcarPalavras(id, request.itens()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR')")
    public AvaliacaoResponse buscar(@PathVariable Long id) {
        return AvaliacaoResponse.from(avaliacaoService.buscar(id));
    }

    @GetMapping("/{id}/auditoria")
    @PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR')")
    public List<AvaliacaoAuditoriaResponse> consultarAuditoria(@PathVariable Long id) {
        return avaliacaoService.consultarAuditoria(id).stream().map(AvaliacaoAuditoriaResponse::from).toList();
    }

    /** AVA-27..AVA-30: o {@code mimeType} vem do {@code Content-Type} do próprio arquivo enviado. */
    @PostMapping("/{id}/audio")
    @PreAuthorize("hasRole('PROFESSOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public void enviarAudio(@PathVariable Long id, @RequestParam("audio") MultipartFile audio) throws IOException {
        avaliacaoService.enviarAudio(id, audio.getBytes(), audio.getContentType());
    }

    /** AVA-31, AVA-32: {@code Content-Type} da resposta é o {@code mimeType} gravado no envio. */
    @GetMapping("/{id}/audio")
    @PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR')")
    public ResponseEntity<byte[]> baixarAudio(@PathVariable Long id) {
        AvaliacaoService.AudioBaixado audio = avaliacaoService.baixarAudio(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(audio.mimeType()))
                .body(audio.conteudo());
    }
}
