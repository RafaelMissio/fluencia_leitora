package com.missio.fluencia_leitora.cadastros.aluno;

import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CAD-16: AlunoRepository.buscarPorNome encontra "João Silva" ao buscar
 * "joao" (sem acento, minúsculo) sem incluir "Joana" incorretamente,
 * confirmando contra MySQL real que a collation {@code utf8mb4_0900_ai_ci}
 * (design.md "Risks &amp; Concerns") resolve o risco de busca
 * case/accent-insensitive; pagina e ordena por nome; e
 * buscarPorNomeEProfessor restringe aos alunos com matrícula no ano letivo
 * ATIVO daquele professor.
 */
@Transactional
class AlunoRepositoryIT extends IntegrationTestBase {

    @Autowired
    private AlunoRepository alunoRepository;

    @Autowired
    private AnoLetivoRepository anoLetivoRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AnoLetivo novoAnoLetivo(int ano) {
        return anoLetivoRepository.save(new AnoLetivo(ano, LocalDate.of(ano, 2, 1), LocalDate.of(ano, 12, 15)));
    }

    private void matricular(Aluno aluno, AnoLetivo anoLetivo, Turma turma, Professor professor) {
        jdbcTemplate.update(
                "INSERT INTO matricula (aluno_id, ano_letivo_id, turma_id, professor_id, serie, ano_finalizado, version) "
                        + "VALUES (?, ?, ?, ?, ?, false, 0)",
                aluno.getId(), anoLetivo.getId(), turma.getId(), professor.getId(), turma.getSerie());
    }

    @Test
    void buscarPorNomeEncontraComAcentoECaseInsensitiveSemIncluirNomeDiferente() {
        // Nomes com um sufixo exclusivo deste teste (RepoColacao) para não
        // colidir com dados de outras classes de IT que não fazem rollback
        // (ex.: AlunoControllerIT), já que o banco do Testcontainers é
        // compartilhado por toda a suíte.
        alunoRepository.save(new Aluno("João RepoColacao Silva"));
        alunoRepository.save(new Aluno("Joana RepoColacao"));

        Page<Aluno> resultado = alunoRepository.buscarPorNome("joao repocolacao", PageRequest.of(0, 20));

        assertEquals(1, resultado.getTotalElements());
        assertEquals("João RepoColacao Silva", resultado.getContent().get(0).getNome());
    }

    @Test
    void buscarPorNomePaginaEOrdenaPorNome() {
        List<String> nomes = List.of(
                "Paginacao Zeta", "Paginacao Delta", "Paginacao Alfa", "Paginacao Gama", "Paginacao Beta");
        nomes.forEach(nome -> alunoRepository.save(new Aluno(nome)));

        Page<Aluno> primeiraPagina = alunoRepository.buscarPorNome("Paginacao", PageRequest.of(0, 3));

        assertEquals(5, primeiraPagina.getTotalElements());
        assertEquals(3, primeiraPagina.getContent().size());
        assertEquals(
                List.of("Paginacao Alfa", "Paginacao Beta", "Paginacao Delta"),
                primeiraPagina.getContent().stream().map(Aluno::getNome).toList());
    }

    @Test
    void buscarPorNomeSemResultadoRetornaPaginaVazia() {
        Page<Aluno> resultado = alunoRepository.buscarPorNome("termo-sem-correspondencia-xyz", PageRequest.of(0, 20));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void buscarPorNomeEProfessorRetornaSoAlunosComMatriculaAtivaDoProfessor() {
        AnoLetivo anoLetivo = novoAnoLetivo(2470);
        anoLetivo.setSituacao(SituacaoAnoLetivo.ATIVO);
        anoLetivoRepository.save(anoLetivo);
        Professor professorA = professorRepository.save(new Professor("Professor A"));
        Professor professorB = professorRepository.save(new Professor("Professor B"));
        Turma turmaA = turmaRepository.save(new Turma("Turma Prof A", 1, anoLetivo, professorA));
        Turma turmaB = turmaRepository.save(new Turma("Turma Prof B", 1, anoLetivo, professorB));

        Aluno alunoDoProfessorA = alunoRepository.save(new Aluno("Escopo Ana"));
        Aluno alunoDoProfessorB = alunoRepository.save(new Aluno("Escopo Beto"));
        matricular(alunoDoProfessorA, anoLetivo, turmaA, professorA);
        matricular(alunoDoProfessorB, anoLetivo, turmaB, professorB);

        Page<Aluno> resultado = alunoRepository.buscarPorNomeEProfessor("Escopo", professorA.getId(), PageRequest.of(0, 20));

        assertEquals(1, resultado.getTotalElements());
        assertEquals(alunoDoProfessorA.getId(), resultado.getContent().get(0).getId());
    }
}
