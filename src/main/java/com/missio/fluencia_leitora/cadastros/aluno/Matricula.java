package com.missio.fluencia_leitora.cadastros.aluno;

import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
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

@Entity
@Table(name = "matricula")
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ano_letivo_id", nullable = false)
    private AnoLetivo anoLetivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turma_id", nullable = false)
    private Turma turma;

    @Column(nullable = false)
    private int serie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    private Professor professor;

    @Column(name = "ano_finalizado", nullable = false)
    private boolean anoFinalizado = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusMatricula status = StatusMatricula.CURSANDO;

    @Version
    private Long version;

    protected Matricula() {
    }

    public Matricula(Aluno aluno, AnoLetivo anoLetivo, Turma turma, int serie, Professor professor) {
        this.aluno = aluno;
        this.anoLetivo = anoLetivo;
        this.turma = turma;
        this.serie = serie;
        this.professor = professor;
    }

    public Long getId() {
        return id;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public AnoLetivo getAnoLetivo() {
        return anoLetivo;
    }

    public Turma getTurma() {
        return turma;
    }

    public void setTurma(Turma turma) {
        this.turma = turma;
    }

    public int getSerie() {
        return serie;
    }

    public void setSerie(int serie) {
        this.serie = serie;
    }

    public Professor getProfessor() {
        return professor;
    }

    public void setProfessor(Professor professor) {
        this.professor = professor;
    }

    public boolean isAnoFinalizado() {
        return anoFinalizado;
    }

    public void setAnoFinalizado(boolean anoFinalizado) {
        this.anoFinalizado = anoFinalizado;
    }

    public StatusMatricula getStatus() {
        return status;
    }

    /** Mantém {@code anoFinalizado} coerente: finalizado quando aprovado ou reprovado. */
    public void setStatus(StatusMatricula status) {
        this.status = status;
        this.anoFinalizado = status != StatusMatricula.CURSANDO;
    }

    public Long getVersion() {
        return version;
    }
}
