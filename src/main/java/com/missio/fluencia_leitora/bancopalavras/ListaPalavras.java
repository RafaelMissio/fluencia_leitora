package com.missio.fluencia_leitora.bancopalavras;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Lista reutilizável de palavras, pseudopalavras ou um texto curto, por
 * série e tipo de leitura (PAL-01..PAL-12). Agregado único: {@link
 * ItemListaPalavras} só existe dentro do ciclo de vida de uma lista (ver
 * design.md, Data Models).
 */
@Entity
@Table(name = "lista_palavras")
public class ListaPalavras {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false)
    private int serie;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_leitura", nullable = false)
    private TipoLeituraCodigo tipoLeitura;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_palavra")
    private TipoPalavra tipoPalavra;

    @Column(length = 2000)
    private String texto;

    @Column(nullable = false)
    private boolean ativo = true;

    @OneToMany(mappedBy = "lista", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<ItemListaPalavras> itens = new ArrayList<>();

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected ListaPalavras() {
    }

    public ListaPalavras(String nome, int serie, TipoLeituraCodigo tipoLeitura, TipoPalavra tipoPalavra, String texto) {
        this.nome = nome;
        this.serie = serie;
        this.tipoLeitura = tipoLeitura;
        this.tipoPalavra = tipoPalavra;
        this.texto = texto;
    }

    /** Cria e associa um novo item ao final da coleção {@link #itens}. */
    public void adicionarItem(String palavra, TipoPalavra tipoPalavraItem, int ordem) {
        itens.add(new ItemListaPalavras(this, palavra, tipoPalavraItem, ordem));
    }

    /**
     * Substitui toda a coleção de itens: os antigos são removidos (o
     * {@code orphanRemoval} cuida da exclusão) e os novos são associados a
     * esta lista.
     */
    public void substituirItens(List<ItemListaPalavras> novosItens) {
        itens.clear();
        novosItens.forEach(item -> itens.add(new ItemListaPalavras(this, item.getPalavra(), item.getTipoPalavra(), item.getOrdem())));
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getSerie() {
        return serie;
    }

    public void setSerie(int serie) {
        this.serie = serie;
    }

    public TipoLeituraCodigo getTipoLeitura() {
        return tipoLeitura;
    }

    public void setTipoLeitura(TipoLeituraCodigo tipoLeitura) {
        this.tipoLeitura = tipoLeitura;
    }

    public TipoPalavra getTipoPalavra() {
        return tipoPalavra;
    }

    public void setTipoPalavra(TipoPalavra tipoPalavra) {
        this.tipoPalavra = tipoPalavra;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public List<ItemListaPalavras> getItens() {
        return itens;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
