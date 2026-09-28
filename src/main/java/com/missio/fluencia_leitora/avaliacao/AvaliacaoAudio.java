package com.missio.fluencia_leitora.avaliacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * A referência (não os bytes) do áudio de uma avaliação - entidade separada
 * com FK única para {@code avaliacao} (design.md, Components e Tech
 * Decisions: sem {@code nomeArquivo}/{@code duracaoSegundos}, nenhum AC os
 * popula).
 */
@Entity
@Table(name = "avaliacao_audio")
public class AvaliacaoAudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliacao_id", nullable = false, unique = true)
    private Avaliacao avaliacao;

    @Column(name = "referencia_armazenamento", nullable = false, length = 500)
    private String referenciaArmazenamento;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "tamanho_bytes", nullable = false)
    private long tamanhoBytes;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected AvaliacaoAudio() {
    }

    public AvaliacaoAudio(Avaliacao avaliacao, String referenciaArmazenamento, String mimeType, long tamanhoBytes) {
        this.avaliacao = avaliacao;
        this.referenciaArmazenamento = referenciaArmazenamento;
        this.mimeType = mimeType;
        this.tamanhoBytes = tamanhoBytes;
    }

    public Long getId() {
        return id;
    }

    public Avaliacao getAvaliacao() {
        return avaliacao;
    }

    public String getReferenciaArmazenamento() {
        return referenciaArmazenamento;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
