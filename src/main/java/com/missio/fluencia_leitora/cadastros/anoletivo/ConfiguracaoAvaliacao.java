package com.missio.fluencia_leitora.cadastros.anoletivo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "configuracao_avaliacao")
public class ConfiguracaoAvaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ano_letivo_id", nullable = false)
    private AnoLetivo anoLetivo;

    @Column(nullable = false)
    private int serie;

    @Column(name = "quantidade_minima", nullable = false)
    private int quantidadeMinima;

    @Column(name = "quantidade_maxima", nullable = false)
    private int quantidadeMaxima;

    @Version
    private Long version;

    protected ConfiguracaoAvaliacao() {
    }

    public ConfiguracaoAvaliacao(AnoLetivo anoLetivo, int serie, int quantidadeMinima, int quantidadeMaxima) {
        this.anoLetivo = anoLetivo;
        this.serie = serie;
        this.quantidadeMinima = quantidadeMinima;
        this.quantidadeMaxima = quantidadeMaxima;
    }

    public Long getId() {
        return id;
    }

    public AnoLetivo getAnoLetivo() {
        return anoLetivo;
    }

    public int getSerie() {
        return serie;
    }

    public int getQuantidadeMinima() {
        return quantidadeMinima;
    }

    public void setQuantidadeMinima(int quantidadeMinima) {
        this.quantidadeMinima = quantidadeMinima;
    }

    public int getQuantidadeMaxima() {
        return quantidadeMaxima;
    }

    public void setQuantidadeMaxima(int quantidadeMaxima) {
        this.quantidadeMaxima = quantidadeMaxima;
    }

    public Long getVersion() {
        return version;
    }
}
