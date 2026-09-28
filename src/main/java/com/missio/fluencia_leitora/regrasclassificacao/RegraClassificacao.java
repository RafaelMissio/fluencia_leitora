package com.missio.fluencia_leitora.regrasclassificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Uma faixa (banda de acertos) de uma série, com a fase/nível resultantes
 * (design.md, Components). Faixas antigas nunca são apagadas: {@link
 * #inativar(Long, Instant)} apenas marca {@code ativo=false} e grava quem/
 * quando (REG-13).
 */
@Entity
@Table(name = "regra_classificacao")
public class RegraClassificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int serie;

    @Column(name = "quantidade_minima_acertos", nullable = false)
    private int quantidadeMinimaAcertos;

    @Column(name = "quantidade_maxima_acertos")
    private Integer quantidadeMaximaAcertos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Fase fase;

    @Column
    private Integer nivel;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "alterado_por")
    private Long alteradoPor;

    @Column(name = "alterado_em")
    private Instant alteradoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected RegraClassificacao() {
    }

    public RegraClassificacao(
            int serie, int quantidadeMinimaAcertos, Integer quantidadeMaximaAcertos, Fase fase, Integer nivel) {
        this.serie = serie;
        this.quantidadeMinimaAcertos = quantidadeMinimaAcertos;
        this.quantidadeMaximaAcertos = quantidadeMaximaAcertos;
        this.fase = fase;
        this.nivel = nivel;
    }

    /** Marca esta faixa como substituída: {@code ativo=false} + auditoria, numa só chamada (REG-13). */
    public void inativar(Long alteradoPor, Instant agora) {
        this.ativo = false;
        this.alteradoPor = alteradoPor;
        this.alteradoEm = agora;
    }

    public Long getId() {
        return id;
    }

    public int getSerie() {
        return serie;
    }

    public int getQuantidadeMinimaAcertos() {
        return quantidadeMinimaAcertos;
    }

    public Integer getQuantidadeMaximaAcertos() {
        return quantidadeMaximaAcertos;
    }

    public Fase getFase() {
        return fase;
    }

    public Integer getNivel() {
        return nivel;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Long getAlteradoPor() {
        return alteradoPor;
    }

    public Instant getAlteradoEm() {
        return alteradoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
