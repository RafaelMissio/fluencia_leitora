package com.missio.fluencia_leitora.cadastros.aluno;

import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.common.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CAD-12/CAD-13: nova matrícula do mesmo aluno em outro ano letivo, rejeitando
 * duplicidade no mesmo ano letivo e turma inválida/ano encerrado.
 */
@Service
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final TurmaRepository turmaRepository;
    private final AlunoRepository alunoRepository;
    private final ProfessorRepository professorRepository;

    public MatriculaService(
            MatriculaRepository matriculaRepository,
            TurmaRepository turmaRepository,
            AlunoRepository alunoRepository,
            ProfessorRepository professorRepository) {
        this.matriculaRepository = matriculaRepository;
        this.turmaRepository = turmaRepository;
        this.alunoRepository = alunoRepository;
        this.professorRepository = professorRepository;
    }

    @Transactional
    public Matricula matricular(Long alunoId, Long turmaId) {
        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ALUNO_NAO_ENCONTRADO", "Aluno não encontrado"));
        Turma turma = AlunoService.buscarTurmaValidaParaMatricula(turmaId, turmaRepository);

        if (matriculaRepository.existsByAlunoIdAndAnoLetivoId(alunoId, turma.getAnoLetivo().getId())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "MATRICULA_DUPLICADA", "Aluno já tem matrícula nesse ano letivo");
        }

        return matriculaRepository.save(
                new Matricula(aluno, turma.getAnoLetivo(), turma, turma.getSerie(), turma.getProfessor()));
    }

    /**
     * CAD-14/CAD-17: troca o professor e/ou transfere a matrícula para outra
     * turma do mesmo ano letivo (atualizando a série), e marca
     * {@code anoFinalizado}; nenhuma dessas alterações toca em avaliações
     * anteriores (não existem nesta feature - este service não grava em
     * nenhuma outra tabela além de {@code matricula}). Parâmetros nulos
     * deixam o respectivo campo inalterado (atualização parcial via PATCH).
     */
    @Transactional
    public Matricula atualizar(Long matriculaId, Long novoProfessorId, Long novaTurmaId, Boolean anoFinalizado) {
        Matricula matricula = matriculaRepository.findById(matriculaId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "MATRICULA_NAO_ENCONTRADA", "Matrícula não encontrada"));

        if (novoProfessorId != null) {
            matricula.setProfessor(buscarProfessorAtivo(novoProfessorId));
        }
        if (novaTurmaId != null) {
            Turma novaTurma = buscarTurmaAtiva(novaTurmaId);
            matricula.setTurma(novaTurma);
            matricula.setSerie(novaTurma.getSerie());
        }
        if (anoFinalizado != null) {
            matricula.setAnoFinalizado(anoFinalizado);
        }

        return matriculaRepository.save(matricula);
    }

    private Professor buscarProfessorAtivo(Long professorId) {
        Professor professor = professorRepository.findById(professorId).orElse(null);
        if (professor == null || !professor.isAtivo()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "REFERENCIA_INVALIDA", "professorId inválido ou inativo");
        }
        return professor;
    }

    private Turma buscarTurmaAtiva(Long turmaId) {
        Turma turma = turmaRepository.findById(turmaId).orElse(null);
        if (turma == null || !turma.isAtivo()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "TURMA_INVALIDA", "turmaId inválido ou inativo");
        }
        return turma;
    }
}
