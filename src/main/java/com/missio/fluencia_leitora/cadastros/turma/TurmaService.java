package com.missio.fluencia_leitora.cadastros.turma;

import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
import com.missio.fluencia_leitora.common.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CAD-07/CAD-10: cadastro de turma vinculada a um ano letivo e a um
 * professor opcional, e troca do professor responsável sem afetar
 * avaliações já criadas.
 *
 * <p>SPEC_DEVIATION: {@code criar}/{@code atualizarProfessor} recebem
 * parâmetros primitivos em vez de um {@code CriarTurmaRequest} (mencionado
 * no texto da T16), porque esse DTO só é criado na T18 (Where da T16 lista
 * somente {@code TurmaService.java}). Mesmo padrão já usado por {@code
 * AnoLetivoService}/{@code ConfiguracaoAvaliacaoService} nas T9/T10.
 *
 * <p>SPEC_DEVIATION: {@code inativar(Long id)} não está no texto original da
 * T16, mas o design.md (seção {@code cadastros.turma}) atribui o soft-delete
 * de turma a este service, e a T18 ({@code DELETE /turmas/{id}}) depende
 * dele. Mesmo padrão já usado por {@code AnoLetivoService.inativar} na T11.
 */
@Service
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final ProfessorRepository professorRepository;
    private final AnoLetivoRepository anoLetivoRepository;

    public TurmaService(
            TurmaRepository turmaRepository,
            ProfessorRepository professorRepository,
            AnoLetivoRepository anoLetivoRepository) {
        this.turmaRepository = turmaRepository;
        this.professorRepository = professorRepository;
        this.anoLetivoRepository = anoLetivoRepository;
    }

    /** Listagem para a tela de cadastro (frontend-web T31): só turmas ativas, por nome. */
    @Transactional(readOnly = true)
    public List<Turma> listar() {
        return turmaRepository.findByAtivoTrueOrderByNomeAsc();
    }

    @Transactional
    public Turma criar(String nome, int serie, Long anoLetivoId, Long professorId) {
        AnoLetivo anoLetivo = buscarAnoLetivoAtivo(anoLetivoId);
        Professor professor = professorId == null ? null : buscarProfessorAtivo(professorId);

        if (turmaRepository.existsByNomeIgnoreCaseAndAnoLetivoId(nome, anoLetivoId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "TURMA_DUPLICADA", "Já existe uma turma com esse nome nesse ano letivo");
        }

        return turmaRepository.save(new Turma(nome, serie, anoLetivo, professor));
    }

    @Transactional
    public Turma atualizarProfessor(Long turmaId, Long novoProfessorId) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "TURMA_NAO_ENCONTRADA", "Turma não encontrada"));

        Professor novoProfessor = buscarProfessorAtivo(novoProfessorId);
        turma.setProfessor(novoProfessor);
        return turmaRepository.save(turma);
    }

    /** CAD-19/RNF006: soft-delete - seta {@code ativo=false}, nunca exclui a linha. */
    @Transactional
    public void inativar(Long id) {
        Turma turma = turmaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "TURMA_NAO_ENCONTRADA", "Turma não encontrada"));
        turma.setAtivo(false);
        turmaRepository.save(turma);
    }

    private AnoLetivo buscarAnoLetivoAtivo(Long anoLetivoId) {
        AnoLetivo anoLetivo = anoLetivoRepository.findById(anoLetivoId).orElse(null);
        if (anoLetivo == null || !anoLetivo.isAtivo()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "REFERENCIA_INVALIDA", "anoLetivoId inválido ou inativo");
        }
        return anoLetivo;
    }

    private Professor buscarProfessorAtivo(Long professorId) {
        Professor professor = professorRepository.findById(professorId).orElse(null);
        if (professor == null || !professor.isAtivo()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "REFERENCIA_INVALIDA", "professorId inválido ou inativo");
        }
        return professor;
    }
}
