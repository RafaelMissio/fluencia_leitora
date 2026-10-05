package com.missio.fluencia_leitora.cadastros.aluno;

import java.util.Comparator;
import java.util.List;
import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.security.ContextoUsuarioPort;
import com.missio.fluencia_leitora.common.security.Perfil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final HistoricoAvaliacaoPort historicoAvaliacaoPort;

    public AlunoService(
            AlunoRepository alunoRepository,
            MatriculaRepository matriculaRepository,
            TurmaRepository turmaRepository,
            HistoricoAvaliacaoPort historicoAvaliacaoPort) {
        this.alunoRepository = alunoRepository;
        this.matriculaRepository = matriculaRepository;
        this.turmaRepository = turmaRepository;
        this.historicoAvaliacaoPort = historicoAvaliacaoPort;
    }

    @Transactional
    public AlunoComMatricula criarComMatricula(String nome, Long turmaId) {
        Turma turma = buscarTurmaValidaParaMatricula(turmaId, turmaRepository);

        Aluno aluno = alunoRepository.save(new Aluno(nome));
        Matricula matricula = matriculaRepository.save(
                new Matricula(aluno, turma.getAnoLetivo(), turma, turma.getSerie(), turma.getProfessor()));

        return new AlunoComMatricula(aluno, matricula);
    }

    private static final int TERMO_MINIMO = 2;

    /**
     * CAD-16: busca por nome, restrita aos alunos com matrícula no ano
     * letivo ATIVO cujo professor é o do contexto quando o perfil autenticado
     * é PROFESSOR; sem restrição para COORDENADOR.
     */
    @Transactional(readOnly = true)
    public Page<AlunoBusca> buscar(String termo, Pageable pageable, ContextoUsuarioPort contexto) {
        if (termo == null || termo.length() < TERMO_MINIMO) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "TERMO_INVALIDO", "O termo de busca deve ter ao menos 2 caracteres");
        }

        Page<Aluno> pagina = contexto.perfilAtual() == Perfil.PROFESSOR
                ? alunoRepository.buscarPorNomeEProfessor(termo, contexto.professorIdAtual(), pageable)
                : alunoRepository.buscarPorNome(termo, pageable);

        return pagina.map(aluno -> comMatriculas(aluno));
    }

    /**
     * AUTH-09: aluno por id com a matrícula do ano letivo ATIVO (ou
     * {@code null}), usada pelo controller para checar se o PROFESSOR é o dono.
     */
    @Transactional(readOnly = true)
    public AlunoBusca buscarPorId(Long id) {
        Aluno aluno = buscarAlunoExistente(id);
        return comMatriculas(aluno);
    }

    private AlunoBusca comMatriculas(Aluno aluno) {
        List<Matricula> matriculas = matriculaRepository.findByAlunoId(aluno.getId()).stream()
                .sorted(Comparator.comparingInt((Matricula m) -> m.getAnoLetivo().getAno()).reversed())
                .toList();
        Matricula ativa = matriculas.stream()
                .filter(matricula -> matricula.getAnoLetivo().getSituacao() == SituacaoAnoLetivo.ATIVO)
                .findFirst()
                .orElse(null);
        return new AlunoBusca(aluno, ativa, matriculas);
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

    /**
     * CAD-15: bloqueia a troca de nome quando o aluno tem pelo menos uma
     * avaliação com status diferente de CANCELADA.
     */
    @Transactional
    public Aluno atualizarNome(Long id, String novoNome) {
        Aluno aluno = buscarAlunoExistente(id);

        if (historicoAvaliacaoPort.existeAvaliacaoNaoCancelada(id)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "ALUNO_COM_AVALIACAO", "Aluno possui avaliação não cancelada");
        }

        aluno.setNome(novoNome);
        return alunoRepository.save(aluno);
    }

    /** CAD-19/RNF006: soft-delete - seta {@code ativo=false}, nunca exclui a linha. */
    @Transactional
    public void inativar(Long id) {
        Aluno aluno = buscarAlunoExistente(id);
        aluno.setAtivo(false);
        alunoRepository.save(aluno);
    }

    /** Ativa ou inativa o aluno (soft-delete reversível). */
    @Transactional
    public Aluno alterarAtivo(Long id, boolean ativo) {
        Aluno aluno = buscarAlunoExistente(id);
        aluno.setAtivo(ativo);
        return alunoRepository.save(aluno);
    }

    private Aluno buscarAlunoExistente(Long id) {
        return alunoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ALUNO_NAO_ENCONTRADO", "Aluno não encontrado"));
    }

    /** CAD-11: par aluno + matrícula criados por {@link #criarComMatricula}. */
    public record AlunoComMatricula(Aluno aluno, Matricula matricula) {
    }

    /**
     * CAD-16: item de resultado de {@link #buscar}. {@code matriculaAtiva} é
     * {@code null} quando o aluno não tem matrícula no ano letivo ATIVO.
     */
    public record AlunoBusca(Aluno aluno, Matricula matriculaAtiva, List<Matricula> matriculas) {

        public AlunoBusca(Aluno aluno, Matricula matriculaAtiva) {
            this(aluno, matriculaAtiva, matriculaAtiva == null ? List.of() : List.of(matriculaAtiva));
        }
    }
}
