package com.missio.fluencia_leitora.avaliacao;

import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.bancopalavras.TipoPalavra;
import com.missio.fluencia_leitora.cadastros.aluno.Aluno;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.dominio.Ciclo;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.regrasclassificacao.Fase;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Agregado raiz: configuração, cópias da matrícula, estado do cronômetro,
 * palavras e resultado/classificação de uma avaliação de leitura
 * (design.md, Components e Data Models). {@link PalavraAvaliacao} só existe
 * dentro do ciclo de vida deste agregado.
 */
@Entity
@Table(name = "avaliacao")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    private Professor professor;

    @Column(name = "professor_nome", length = 150)
    private String professorNome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turma_id", nullable = false)
    private Turma turma;

    @Column(name = "turma_nome", nullable = false, length = 100)
    private String turmaNome;

    @Column(nullable = false)
    private int serie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ano_letivo_id", nullable = false)
    private AnoLetivo anoLetivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciclo_id", nullable = false)
    private Ciclo ciclo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_leitura", nullable = false)
    private TipoLeituraCodigo tipoLeitura;

    @Column(name = "data_avaliacao", nullable = false)
    private LocalDate dataAvaliacao;

    @Column(name = "tempo_configurado_segundos", nullable = false)
    private int tempoConfiguradoSegundos;

    @Column(name = "tempo_acumulado_segundos", nullable = false)
    private int tempoAcumuladoSegundos = 0;

    @Column(name = "iniciado_em")
    private Instant iniciadoEm;

    @Column(name = "ultima_atividade_em", nullable = false)
    private Instant ultimaAtividadeEm;

    @Column(name = "finalizado_em")
    private Instant finalizadoEm;

    @Column(name = "tempo_utilizado_segundos")
    private Integer tempoUtilizadoSegundos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAvaliacao status = StatusAvaliacao.CRIADA;

    @Column(name = "quantidade_total", nullable = false)
    private int quantidadeTotal = 0;

    @Column(name = "quantidade_corretas", nullable = false)
    private int quantidadeCorretas = 0;

    @Column(name = "quantidade_incorretas", nullable = false)
    private int quantidadeIncorretas = 0;

    @Column(name = "quantidade_nao_lidas", nullable = false)
    private int quantidadeNaoLidas = 0;

    @Column(name = "percentual_acerto")
    private BigDecimal percentualAcerto;

    @Enumerated(EnumType.STRING)
    @Column
    private Fase fase;

    @Column
    private Integer nivel;

    /**
     * {@code PalavraAvaliacao} não tem {@code @ManyToOne} de volta para este
     * agregado (ver PalavraAvaliacao.java, SPEC_DEVIATION) - a relação é
     * unidirecional, mapeada aqui via {@code @JoinColumn}.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "avaliacao_id", nullable = false)
    @OrderBy("ordem")
    private List<PalavraAvaliacao> palavras = new ArrayList<>();

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Avaliacao() {
    }

    public Avaliacao(
            Aluno aluno,
            Professor professor,
            String professorNome,
            Turma turma,
            String turmaNome,
            int serie,
            AnoLetivo anoLetivo,
            Ciclo ciclo,
            TipoLeituraCodigo tipoLeitura,
            LocalDate dataAvaliacao,
            int tempoConfiguradoSegundos) {
        this.aluno = aluno;
        this.professor = professor;
        this.professorNome = professorNome;
        this.turma = turma;
        this.turmaNome = turmaNome;
        this.serie = serie;
        this.anoLetivo = anoLetivo;
        this.ciclo = ciclo;
        this.tipoLeitura = tipoLeitura;
        this.dataAvaliacao = dataAvaliacao;
        this.tempoConfiguradoSegundos = tempoConfiguradoSegundos;
        this.ultimaAtividadeEm = Instant.now();
    }

    /** Cria e associa uma nova palavra ao final da coleção {@link #palavras}. */
    public void adicionarPalavra(String palavra, TipoPalavra tipoPalavra) {
        palavras.add(new PalavraAvaliacao(palavra, tipoPalavra, palavras.size() + 1));
        quantidadeTotal = palavras.size();
    }

    /** Atualiza {@link #ultimaAtividadeEm} para agora (design.md, Tech Decisions). */
    public void tocarAtividade() {
        this.ultimaAtividadeEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public Professor getProfessor() {
        return professor;
    }

    public String getProfessorNome() {
        return professorNome;
    }

    public Turma getTurma() {
        return turma;
    }

    public String getTurmaNome() {
        return turmaNome;
    }

    public int getSerie() {
        return serie;
    }

    public AnoLetivo getAnoLetivo() {
        return anoLetivo;
    }

    public Ciclo getCiclo() {
        return ciclo;
    }

    public TipoLeituraCodigo getTipoLeitura() {
        return tipoLeitura;
    }

    public LocalDate getDataAvaliacao() {
        return dataAvaliacao;
    }

    public int getTempoConfiguradoSegundos() {
        return tempoConfiguradoSegundos;
    }

    public int getTempoAcumuladoSegundos() {
        return tempoAcumuladoSegundos;
    }

    public void setTempoAcumuladoSegundos(int tempoAcumuladoSegundos) {
        this.tempoAcumuladoSegundos = tempoAcumuladoSegundos;
    }

    public Instant getIniciadoEm() {
        return iniciadoEm;
    }

    public void setIniciadoEm(Instant iniciadoEm) {
        this.iniciadoEm = iniciadoEm;
    }

    public Instant getUltimaAtividadeEm() {
        return ultimaAtividadeEm;
    }

    public Instant getFinalizadoEm() {
        return finalizadoEm;
    }

    public void setFinalizadoEm(Instant finalizadoEm) {
        this.finalizadoEm = finalizadoEm;
    }

    public Integer getTempoUtilizadoSegundos() {
        return tempoUtilizadoSegundos;
    }

    public void setTempoUtilizadoSegundos(Integer tempoUtilizadoSegundos) {
        this.tempoUtilizadoSegundos = tempoUtilizadoSegundos;
    }

    public StatusAvaliacao getStatus() {
        return status;
    }

    public void setStatus(StatusAvaliacao status) {
        this.status = status;
    }

    public int getQuantidadeTotal() {
        return quantidadeTotal;
    }

    public int getQuantidadeCorretas() {
        return quantidadeCorretas;
    }

    public void setQuantidadeCorretas(int quantidadeCorretas) {
        this.quantidadeCorretas = quantidadeCorretas;
    }

    public int getQuantidadeIncorretas() {
        return quantidadeIncorretas;
    }

    public void setQuantidadeIncorretas(int quantidadeIncorretas) {
        this.quantidadeIncorretas = quantidadeIncorretas;
    }

    public int getQuantidadeNaoLidas() {
        return quantidadeNaoLidas;
    }

    public void setQuantidadeNaoLidas(int quantidadeNaoLidas) {
        this.quantidadeNaoLidas = quantidadeNaoLidas;
    }

    public BigDecimal getPercentualAcerto() {
        return percentualAcerto;
    }

    public void setPercentualAcerto(BigDecimal percentualAcerto) {
        this.percentualAcerto = percentualAcerto;
    }

    public Fase getFase() {
        return fase;
    }

    public void setFase(Fase fase) {
        this.fase = fase;
    }

    public Integer getNivel() {
        return nivel;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public List<PalavraAvaliacao> getPalavras() {
        return palavras;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
