package com.missio.fluencia_leitora.cadastros.aluno;

import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import java.util.List;
import java.util.Objects;
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

    /** Alunos matriculados na turma (com o status de cada matrícula), por nome. */
    @Transactional(readOnly = true)
    public List<Matricula> listarPorTurma(Long turmaId) {
        if (!turmaRepository.existsById(turmaId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TURMA_NAO_ENCONTRADA", "Turma não encontrada");
        }
        return matriculaRepository.findByTurmaIdOrderByAlunoNomeAsc(turmaId);
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
    public Matricula atualizar(Long matriculaId, Long novoProfessorId, Long novaTurmaId, Boolean anoFinalizado, StatusMatricula status) {
        Matricula matricula = matriculaRepository.findById(matriculaId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "MATRICULA_NAO_ENCONTRADA", "Matrícula não encontrada"));

        if (novoProfessorId != null) {
            matricula.setProfessor(buscarProfessorAtivo(novoProfessorId));
        }
        if (novaTurmaId != null) {
            Turma novaTurma = buscarTurmaAtiva(novaTurmaId);
            if (!Objects.equals(novaTurma.getAnoLetivo().getId(), matricula.getAnoLetivo().getId())) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "TURMA_OUTRO_ANO_LETIVO",
                        "A turma deve ser do mesmo ano letivo da matrícula; use nova matrícula para outro ano");
            }
            matricula.setTurma(novaTurma);
            matricula.setSerie(novaTurma.getSerie());
        }
        if (anoFinalizado != null) {
            matricula.setAnoFinalizado(anoFinalizado);
        }
        if (status != null) {
            boolean anoAtivo = matricula.getAnoLetivo().getSituacao() == SituacaoAnoLetivo.ATIVO;
            if (anoAtivo && status != StatusMatricula.CURSANDO) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "STATUS_INVALIDO",
                        "Em ano letivo ativo o aluno está cursando; aprovado/reprovado só após o ano deixar de ser ativo");
            }
            if (!anoAtivo && status == StatusMatricula.CURSANDO) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "STATUS_INVALIDO",
                        "Em ano letivo inativo o status deve ser aprovado ou reprovado");
            }
            matricula.setStatus(status);
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
