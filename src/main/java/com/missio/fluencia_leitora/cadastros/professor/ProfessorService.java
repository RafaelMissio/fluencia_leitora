package com.missio.fluencia_leitora.cadastros.professor;

import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.common.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CAD-07/CAD-08/CAD-09: cadastro de professor, consulta com as turmas ativas
 * associadas e inativação (bloqueada quando há turma ativa vinculada).
 *
 * <p>SPEC_DEVIATION: {@code criar} recebe {@code String nome} em vez de um
 * {@code CriarProfessorRequest} (mencionado no texto da T15), porque esse DTO
 * só é criado na T17 (Where da T15 lista somente {@code ProfessorService.java}).
 * Mesmo padrão já usado por {@code AnoLetivoService}/{@code
 * ConfiguracaoAvaliacaoService} nas T9/T10.
 */
@Service
public class ProfessorService {

    private static final int NOME_MINIMO = 3;
    private static final int NOME_MAXIMO = 150;

    private final ProfessorRepository professorRepository;
    private final TurmaRepository turmaRepository;

    public ProfessorService(ProfessorRepository professorRepository, TurmaRepository turmaRepository) {
        this.professorRepository = professorRepository;
        this.turmaRepository = turmaRepository;
    }

    @Transactional
    public Professor criar(String nome) {
        if (nome == null || nome.length() < NOME_MINIMO || nome.length() > NOME_MAXIMO) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "NOME_INVALIDO", "Nome deve ter entre 3 e 150 caracteres");
        }
        return professorRepository.save(new Professor(nome));
    }

    /** Listagem para a tela de cadastro (frontend-web T32): professores ativos, por nome, cada um com suas turmas ativas. */
    @Transactional(readOnly = true)
    public List<ProfessorComTurmas> listarComTurmas() {
        return professorRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(professor -> new ProfessorComTurmas(
                        professor, turmaRepository.findByProfessorIdAndAtivoTrue(professor.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfessorComTurmas buscarComTurmas(Long id) {
        Professor professor = professorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "PROFESSOR_NAO_ENCONTRADO", "Professor não encontrado"));
        List<Turma> turmasAtivas = turmaRepository.findByProfessorIdAndAtivoTrue(id);
        return new ProfessorComTurmas(professor, turmasAtivas);
    }

    /** CAD-19/RNF006: soft-delete - seta {@code ativo=false}, nunca exclui a linha. */
    @Transactional
    public void inativar(Long id) {
        Professor professor = professorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "PROFESSOR_NAO_ENCONTRADO", "Professor não encontrado"));

        if (!turmaRepository.findByProfessorIdAndAtivoTrue(id).isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "PROFESSOR_COM_TURMA_ATIVA",
                    "Professor é responsável por ao menos uma turma ativa");
        }

        professor.setAtivo(false);
        professorRepository.save(professor);
    }

    /**
     * CAD-08: par professor + turmas ativas associadas, devolvido por
     * {@link #buscarComTurmas(Long)}. Não é um DTO de resposta HTTP (esse é
     * criado na T17); é só o retorno interno do service.
     */
    public record ProfessorComTurmas(Professor professor, List<Turma> turmasAtivas) {
    }
}
