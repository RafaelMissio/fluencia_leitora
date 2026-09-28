package com.missio.fluencia_leitora.avaliacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Um registro imutável de alteração numa avaliação {@code FINALIZADA} ou de
 * um cancelamento (design.md, Components). {@code usuarioId} segue o mesmo
 * padrão de {@code RegraClassificacao.alteradoPor} - coluna simples, sem
 * carregar a entidade {@code Usuario}.
 */
@Entity
@Table(name = "avaliacao_auditoria")
public class AvaliacaoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliacao_id", nullable = false)
    private Avaliacao avaliacao;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @CreationTimestamp
    @Column(name = "data_hora", nullable = false, updatable = false)
    private Instant dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AcaoAuditoria acao;

    @Column(name = "valor_anterior", nullable = false, length = 500)
    private String valorAnterior;

    @Column(name = "valor_novo", nullable = false, length = 500)
    private String valorNovo;

    @Column(length = 500)
    private String justificativa;

    protected AvaliacaoAuditoria() {
    }

    public AvaliacaoAuditoria(
            Avaliacao avaliacao,
            Long usuarioId,
            AcaoAuditoria acao,
            String valorAnterior,
            String valorNovo,
            String justificativa) {
        this.avaliacao = avaliacao;
        this.usuarioId = usuarioId;
        this.acao = acao;
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
        this.justificativa = justificativa;
    }

    public Long getId() {
        return id;
    }

    public Avaliacao getAvaliacao() {
        return avaliacao;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Instant getDataHora() {
        return dataHora;
    }

    public AcaoAuditoria getAcao() {
        return acao;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNovo() {
        return valorNovo;
    }

    public String getJustificativa() {
        return justificativa;
    }
}
