package com.missio.fluencia_leitora.avaliacao;

import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.dominio.Ciclo;
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
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Avaliação configurada pelo coordenador para uma série do ano letivo. Vale
 * para todos os alunos da série: cada um a vê como pendente até um professor
 * aplicá-la, o que gera uma {@link Avaliacao} ligada a esta configuração.
 * O conteúdo é exatamente uma fonte: lista de palavras, palavras digitadas
 * (separadas por espaço, todas canônicas) ou texto.
 */
@Entity
@Table(name = "avaliacao_programada")
public class AvaliacaoProgramada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ano_letivo_id", nullable = false)
    private AnoLetivo anoLetivo;

    @Column(nullable = false)
    private int serie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciclo_id", nullable = false)
    private Ciclo ciclo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_leitura", nullable = false)
    private TipoLeituraCodigo tipoLeitura;

    @Column(name = "tempo_segundos", nullable = false)
    private int tempoSegundos;

    @Column(name = "lista_palavras_id")
    private Long listaPalavrasId;

    @Column(columnDefinition = "TEXT")
    private String palavras;

    @Column(columnDefinition = "TEXT")
    private String texto;

    @Column(name = "max_refazeres", nullable = false)
    private int maxRefazeres = 3;

    @Column(nullable = false)
    private boolean ativa = true;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected AvaliacaoProgramada() {
    }

    public AvaliacaoProgramada(
            String nome,
            AnoLetivo anoLetivo,
            int serie,
            Ciclo ciclo,
            TipoLeituraCodigo tipoLeitura,
            int tempoSegundos,
            Long listaPalavrasId,
            String palavras,
            String texto) {
        this.nome = nome;
        this.anoLetivo = anoLetivo;
        this.serie = serie;
        this.ciclo = ciclo;
        this.tipoLeitura = tipoLeitura;
        this.tempoSegundos = tempoSegundos;
        this.listaPalavrasId = listaPalavrasId;
        this.palavras = palavras;
        this.texto = texto;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public AnoLetivo getAnoLetivo() {
        return anoLetivo;
    }

    public int getSerie() {
        return serie;
    }

    public Ciclo getCiclo() {
        return ciclo;
    }

    public TipoLeituraCodigo getTipoLeitura() {
        return tipoLeitura;
    }

    public int getTempoSegundos() {
        return tempoSegundos;
    }

    public Long getListaPalavrasId() {
        return listaPalavrasId;
    }

    public String getPalavras() {
        return palavras;
    }

    public String getTexto() {
        return texto;
    }

    public int getMaxRefazeres() {
        return maxRefazeres;
    }

    public void setMaxRefazeres(int maxRefazeres) {
        this.maxRefazeres = maxRefazeres;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }
}
