package com.missio.fluencia_leitora.avaliacao;

import com.missio.fluencia_leitora.bancopalavras.TipoPalavra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Uma palavra copiada para a avaliação, na posição {@code ordem} (1..n), com
 * seu status de leitura. Só existe dentro do ciclo de vida de uma {@code
 * Avaliacao} - sem repositório próprio (design.md, Components).
 */
@Entity
@Table(name = "avaliacao_palavra")
public class PalavraAvaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String palavra;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_palavra")
    private TipoPalavra tipoPalavra;

    @Column(nullable = false)
    private int ordem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPalavra status = StatusPalavra.PENDENTE;

    protected PalavraAvaliacao() {
    }

    public PalavraAvaliacao(String palavra, TipoPalavra tipoPalavra, int ordem) {
        this.palavra = palavra;
        this.tipoPalavra = tipoPalavra;
        this.ordem = ordem;
    }

    public Long getId() {
        return id;
    }

    public String getPalavra() {
        return palavra;
    }

    public TipoPalavra getTipoPalavra() {
        return tipoPalavra;
    }

    public int getOrdem() {
        return ordem;
    }

    public StatusPalavra getStatus() {
        return status;
    }

    public void setStatus(StatusPalavra status) {
        this.status = status;
    }
}
