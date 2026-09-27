package com.missio.fluencia_leitora.cadastros.aluno;

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

    public MatriculaService(
            MatriculaRepository matriculaRepository, TurmaRepository turmaRepository, AlunoRepository alunoRepository) {
        this.matriculaRepository = matriculaRepository;
        this.turmaRepository = turmaRepository;
        this.alunoRepository = alunoRepository;
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
}
