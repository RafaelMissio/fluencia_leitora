package com.missio.fluencia_leitora.bancopalavras;

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

/**
 * Um item (palavra) de uma {@link ListaPalavras}, na posição {@code ordem}
 * (1..n). Só existe dentro do ciclo de vida de uma lista - ver design.md,
 * Data Models ("agregado único").
 */
@Entity
@Table(name = "item_lista_palavras")
public class ItemListaPalavras {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_palavras_id", nullable = false)
    private ListaPalavras lista;

    @Column(nullable = false, length = 60)
    private String palavra;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_palavra", nullable = false)
    private TipoPalavra tipoPalavra;

    @Column(nullable = false)
    private int ordem;

    protected ItemListaPalavras() {
    }

    public ItemListaPalavras(ListaPalavras lista, String palavra, TipoPalavra tipoPalavra, int ordem) {
        this.lista = lista;
        this.palavra = palavra;
        this.tipoPalavra = tipoPalavra;
        this.ordem = ordem;
    }

    public Long getId() {
        return id;
    }

    public ListaPalavras getLista() {
        return lista;
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
}
