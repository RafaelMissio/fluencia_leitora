package com.missio.fluencia_leitora.avaliacao;

import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.cadastros.aluno.Aluno;
import com.missio.fluencia_leitora.cadastros.aluno.AlunoRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.dominio.Ciclo;
import com.missio.fluencia_leitora.cadastros.dominio.CicloRepository;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T8: {@code existsByAlunoIdAndStatusNot} ignora só {@code CANCELADA};
 * {@code findByStatusAndUltimaAtividadeEmBefore} só traz {@code
 * EM_ANDAMENTO} com {@code ultimaAtividadeEm} anterior ao limite (AVA-17),
 * exercitados contra MySQL real.
 */
@Transactional
class AvaliacaoRepositoryIT extends IntegrationTestBase {

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Autowired
    private AlunoRepository alunoRepository;

    @Autowired
    private AnoLetivoRepository anoLetivoRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private CicloRepository cicloRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AnoLetivo anoLetivo;
    private Turma turma;
    private Ciclo ciclo;

    private void prepararCadastros(int ano) {
        anoLetivo = anoLetivoRepository.save(new AnoLetivo(ano, LocalDate.of(ano, 2, 1), LocalDate.of(ano, 12, 15)));
        turma = turmaRepository.save(new Turma("Turma Avaliacao " + ano, 2, anoLetivo, null));
        ciclo = cicloRepository.findAll().get(0);
    }

    private Avaliacao novaAvaliacao(Aluno aluno, StatusAvaliacao status) {
        Avaliacao avaliacao = new Avaliacao(
                aluno, null, null, turma, turma.getNome(), 2, anoLetivo, ciclo,
                TipoLeituraCodigo.PALAVRA, LocalDate.of(anoLetivo.getAno(), 3, 1), 60);
        avaliacao.adicionarPalavra("gato", null);
        avaliacao.setStatus(status);
        return avaliacaoRepository.saveAndFlush(avaliacao);
    }

    private void definirUltimaAtividade(Avaliacao avaliacao, Instant instante) {
        jdbcTemplate.update(
                "UPDATE avaliacao SET ultima_atividade_em = ? WHERE id = ?",
                Timestamp.from(instante), avaliacao.getId());
    }

    @Test
    void existsByAlunoIdAndStatusNotRetornaFalseSemNenhumaAvaliacao() {
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Sem Avaliacao"));

        assertFalse(avaliacaoRepository.existsByAlunoIdAndStatusNot(aluno.getId(), StatusAvaliacao.CANCELADA));
    }

    @Test
    void existsByAlunoIdAndStatusNotRetornaFalseQuandoSoExisteCancelada() {
        prepararCadastros(2490);
        Aluno aluno = alunoRepository.save(new Aluno("Aluno So Cancelada"));
        novaAvaliacao(aluno, StatusAvaliacao.CANCELADA);

        assertFalse(avaliacaoRepository.existsByAlunoIdAndStatusNot(aluno.getId(), StatusAvaliacao.CANCELADA));
    }

    @Test
    void existsByAlunoIdAndStatusNotRetornaTrueComAvaliacaoNaoCancelada() {
        prepararCadastros(2491);
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Com Avaliacao"));
        Aluno outroAluno = alunoRepository.save(new Aluno("Outro Aluno"));
        novaAvaliacao(aluno, StatusAvaliacao.CANCELADA);
        novaAvaliacao(aluno, StatusAvaliacao.FINALIZADA);
        novaAvaliacao(outroAluno, StatusAvaliacao.CANCELADA);

        assertTrue(avaliacaoRepository.existsByAlunoIdAndStatusNot(aluno.getId(), StatusAvaliacao.CANCELADA));
        assertFalse(avaliacaoRepository.existsByAlunoIdAndStatusNot(outroAluno.getId(), StatusAvaliacao.CANCELADA));
    }

    @Test
    void findByStatusAndUltimaAtividadeEmBeforeTrazSoEmAndamentoAnterioresAoLimite() {
        prepararCadastros(2492);
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Inativas"));
        Instant agora = Instant.now();
        Instant limite = agora.minus(Duration.ofHours(24));

        Avaliacao emAndamentoAntiga = novaAvaliacao(aluno, StatusAvaliacao.EM_ANDAMENTO);
        definirUltimaAtividade(emAndamentoAntiga, agora.minus(Duration.ofHours(48)));
        Avaliacao pausadaAntiga = novaAvaliacao(aluno, StatusAvaliacao.PAUSADA);
        definirUltimaAtividade(pausadaAntiga, agora.minus(Duration.ofHours(48)));
        Avaliacao emAndamentoRecente = novaAvaliacao(aluno, StatusAvaliacao.EM_ANDAMENTO);
        definirUltimaAtividade(emAndamentoRecente, agora.minus(Duration.ofHours(1)));

        List<Long> ids = avaliacaoRepository
                .findByStatusAndUltimaAtividadeEmBefore(StatusAvaliacao.EM_ANDAMENTO, limite)
                .stream()
                .map(Avaliacao::getId)
                .toList();

        assertTrue(ids.contains(emAndamentoAntiga.getId()));
        assertFalse(ids.contains(pausadaAntiga.getId()));
        assertFalse(ids.contains(emAndamentoRecente.getId()));
    }
}
