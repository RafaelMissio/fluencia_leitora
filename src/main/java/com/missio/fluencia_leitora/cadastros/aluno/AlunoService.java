package com.missio.fluencia_leitora.cadastros.aluno;

import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.common.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CAD-11: cadastro de aluno com a primeira matrícula (transação única),
 * copiando série e professor da turma informada.
 *
 * <p>SPEC_DEVIATION: {@code criarComMatricula} recebe parâmetros primitivos
 * em vez de um {@code CriarAlunoRequest} (mencionado no texto da T23), porque
 * esse DTO só é criado na T28 (Where da T23 lista somente {@code
 * AlunoService.java}). Mesmo padrão já usado por {@code AnoLetivoService}/
 * {@code TurmaService}/{@code ProfessorService} em fases anteriores.
 */
@Service
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final MatriculaRepository matriculaRepository;
    private final TurmaRepository turmaRepository;

    public AlunoService(
            AlunoRepository alunoRepository,
            MatriculaRepository matriculaRepository,
            TurmaRepository turmaRepository) {
        this.alunoRepository = alunoRepository;
        this.matriculaRepository = matriculaRepository;
        this.turmaRepository = turmaRepository;
    }

    @Transactional
    public AlunoComMatricula criarComMatricula(String nome, Long turmaId) {
        Turma turma = buscarTurmaValidaParaMatricula(turmaId, turmaRepository);

        Aluno aluno = alunoRepository.save(new Aluno(nome));
        Matricula matricula = matriculaRepository.save(
                new Matricula(aluno, turma.getAnoLetivo(), turma, turma.getSerie(), turma.getProfessor()));

        return new AlunoComMatricula(aluno, matricula);
    }

    /**
     * CAD-11/CAD-12: uma turma só serve para matricular um aluno (novo ou já
     * existente) quando ela está ativa e seu ano letivo não está
     * {@code ENCERRADO} (edge cases da spec, linhas 139-140).
     *
     * <p>Reutilizada por {@link MatriculaService}, que mantém sua própria
     * cópia (mesmo padrão de pequenos helpers privados por serviço já usado
     * em {@code TurmaService}/{@code ProfessorService}).
     */
    static Turma buscarTurmaValidaParaMatricula(Long turmaId, TurmaRepository turmaRepository) {
        Turma turma = turmaRepository.findById(turmaId).orElse(null);
        if (turma == null || !turma.isAtivo()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "TURMA_INVALIDA", "turmaId inválido ou inativo");
        }
        if (turma.getAnoLetivo().getSituacao() == SituacaoAnoLetivo.ENCERRADO) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "ANO_LETIVO_ENCERRADO",
                    "Não é possível matricular em turma de ano letivo encerrado");
        }
        return turma;
    }

    /** CAD-11: par aluno + matrícula criados por {@link #criarComMatricula}. */
    public record AlunoComMatricula(Aluno aluno, Matricula matricula) {
    }
}
