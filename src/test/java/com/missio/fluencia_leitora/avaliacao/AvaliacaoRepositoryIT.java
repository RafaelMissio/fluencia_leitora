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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private AnoLetivo novoAnoLetivo(int ano) {
        return anoLetivoRepository.save(new AnoLetivo(ano, LocalDate.of(ano, 2, 1), LocalDate.of(ano, 12, 15)));
    }

    private Turma novaTurmaEm(AnoLetivo anoLetivoParam, String sufixo) {
        return turmaRepository.save(new Turma("Turma Historico " + sufixo, 2, anoLetivoParam, null));
    }

    private List<Ciclo> ciclosOrdenadosPorId() {
        return cicloRepository.findAll().stream().sorted(Comparator.comparing(Ciclo::getId)).toList();
    }

    private Avaliacao novaAvaliacaoHistorico(
            Aluno aluno,
            AnoLetivo anoLetivoParam,
            Turma turmaParam,
            Ciclo cicloParam,
            TipoLeituraCodigo tipoLeitura,
            LocalDate dataAvaliacao,
            StatusAvaliacao status,
            Instant finalizadoEm) {
        Avaliacao avaliacao = new Avaliacao(
                aluno, null, null, turmaParam, turmaParam.getNome(), 2, anoLetivoParam, cicloParam,
                tipoLeitura, dataAvaliacao, 60);
        avaliacao.adicionarPalavra("gato", null);
        avaliacao.setStatus(status);
        avaliacao.setFinalizadoEm(finalizadoEm);
        return avaliacaoRepository.saveAndFlush(avaliacao);
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

    @Test
    void buscarHistoricoIgnoraCanceladaEEmAndamento() {
        prepararCadastros(2200);
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Historico Status"));
        Avaliacao finalizada = novaAvaliacao(aluno, StatusAvaliacao.FINALIZADA);
        finalizada.setFinalizadoEm(Instant.now());
        avaliacaoRepository.saveAndFlush(finalizada);
        novaAvaliacao(aluno, StatusAvaliacao.CANCELADA);
        novaAvaliacao(aluno, StatusAvaliacao.EM_ANDAMENTO);

        Page<Avaliacao> pagina = avaliacaoRepository.buscarHistorico(
                aluno.getId(), StatusAvaliacao.FINALIZADA, null, null, null, PageRequest.of(0, 20));

        assertEquals(1, pagina.getTotalElements());
        assertEquals(finalizada.getId(), pagina.getContent().get(0).getId());
    }

    @Test
    void buscarHistoricoFiltraPorAnoLetivoIdIsolado() {
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Historico Ano"));
        AnoLetivo ano2201 = novoAnoLetivo(2201);
        AnoLetivo ano2202 = novoAnoLetivo(2202);
        Turma turma2201 = novaTurmaEm(ano2201, "2201");
        Turma turma2202 = novaTurmaEm(ano2202, "2202");
        Ciclo cicloQualquer = ciclosOrdenadosPorId().get(0);

        Avaliacao doAno2201 = novaAvaliacaoHistorico(
                aluno, ano2201, turma2201, cicloQualquer, TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2201, 3, 1), StatusAvaliacao.FINALIZADA, Instant.now());
        novaAvaliacaoHistorico(
                aluno, ano2202, turma2202, cicloQualquer, TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2202, 3, 1), StatusAvaliacao.FINALIZADA, Instant.now());

        Page<Avaliacao> pagina = avaliacaoRepository.buscarHistorico(
                aluno.getId(), StatusAvaliacao.FINALIZADA, ano2201.getId(), null, null, PageRequest.of(0, 20));

        assertEquals(1, pagina.getTotalElements());
        assertEquals(doAno2201.getId(), pagina.getContent().get(0).getId());
    }

    @Test
    void buscarHistoricoFiltraPorTipoLeituraIsolado() {
        prepararCadastros(2203);
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Historico Tipo"));
        Avaliacao dePalavra = novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclo, TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2203, 3, 1), StatusAvaliacao.FINALIZADA, Instant.now());
        novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclo, TipoLeituraCodigo.TEXTO_CURTO,
                LocalDate.of(2203, 3, 2), StatusAvaliacao.FINALIZADA, Instant.now());

        Page<Avaliacao> pagina = avaliacaoRepository.buscarHistorico(
                aluno.getId(), StatusAvaliacao.FINALIZADA, null, TipoLeituraCodigo.PALAVRA, null,
                PageRequest.of(0, 20));

        assertEquals(1, pagina.getTotalElements());
        assertEquals(dePalavra.getId(), pagina.getContent().get(0).getId());
    }

    @Test
    void buscarHistoricoFiltraPorCicloIdIsolado() {
        prepararCadastros(2204);
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Historico Ciclo"));
        List<Ciclo> ciclos = ciclosOrdenadosPorId();
        Avaliacao doCiclo0 = novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclos.get(0), TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2204, 3, 1), StatusAvaliacao.FINALIZADA, Instant.now());
        novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclos.get(1), TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2204, 3, 2), StatusAvaliacao.FINALIZADA, Instant.now());

        Page<Avaliacao> pagina = avaliacaoRepository.buscarHistorico(
                aluno.getId(), StatusAvaliacao.FINALIZADA, null, null, ciclos.get(0).getId(),
                PageRequest.of(0, 20));

        assertEquals(1, pagina.getTotalElements());
        assertEquals(doCiclo0.getId(), pagina.getContent().get(0).getId());
    }

    @Test
    void buscarHistoricoFiltrosCombinados() {
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Historico Combinado"));
        AnoLetivo ano2205 = novoAnoLetivo(2205);
        AnoLetivo ano2206 = novoAnoLetivo(2206);
        Turma turma2205 = novaTurmaEm(ano2205, "2205");
        Turma turma2206 = novaTurmaEm(ano2206, "2206");
        List<Ciclo> ciclos = ciclosOrdenadosPorId();

        Avaliacao alvo = novaAvaliacaoHistorico(
                aluno, ano2205, turma2205, ciclos.get(0), TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2205, 3, 1), StatusAvaliacao.FINALIZADA, Instant.now());
        // mesmo ano e tipo, ciclo diferente
        novaAvaliacaoHistorico(
                aluno, ano2205, turma2205, ciclos.get(1), TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2205, 3, 2), StatusAvaliacao.FINALIZADA, Instant.now());
        // mesmo ano e ciclo, tipo diferente
        novaAvaliacaoHistorico(
                aluno, ano2205, turma2205, ciclos.get(0), TipoLeituraCodigo.TEXTO_CURTO,
                LocalDate.of(2205, 3, 3), StatusAvaliacao.FINALIZADA, Instant.now());
        // mesmo tipo e ciclo, ano diferente
        novaAvaliacaoHistorico(
                aluno, ano2206, turma2206, ciclos.get(0), TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2206, 3, 1), StatusAvaliacao.FINALIZADA, Instant.now());

        Page<Avaliacao> pagina = avaliacaoRepository.buscarHistorico(
                aluno.getId(), StatusAvaliacao.FINALIZADA, ano2205.getId(), TipoLeituraCodigo.PALAVRA,
                ciclos.get(0).getId(), PageRequest.of(0, 20));

        assertEquals(1, pagina.getTotalElements());
        assertEquals(alvo.getId(), pagina.getContent().get(0).getId());
    }

    @Test
    void buscarHistoricoOrdenaPorDataAvaliacaoDescDepoisFinalizadoEmDesc() {
        prepararCadastros(2207);
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Historico Ordem"));
        Instant agora = Instant.now();

        Avaliacao maisAntigaNoDia = novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclo, TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2207, 3, 1), StatusAvaliacao.FINALIZADA, agora.minus(Duration.ofHours(2)));
        Avaliacao maisRecenteNoDia = novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclo, TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2207, 3, 1), StatusAvaliacao.FINALIZADA, agora);
        Avaliacao diaSeguinte = novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclo, TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2207, 3, 2), StatusAvaliacao.FINALIZADA, agora.minus(Duration.ofHours(5)));

        List<Long> ids = avaliacaoRepository
                .buscarHistorico(aluno.getId(), StatusAvaliacao.FINALIZADA, null, null, null, PageRequest.of(0, 20))
                .getContent()
                .stream()
                .map(Avaliacao::getId)
                .toList();

        assertEquals(List.of(diaSeguinte.getId(), maisRecenteNoDia.getId(), maisAntigaNoDia.getId()), ids);
    }

    @Test
    void buscarHistoricoRetornaPaginado() {
        prepararCadastros(2208);
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Historico Paginacao"));
        novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclo, TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2208, 3, 1), StatusAvaliacao.FINALIZADA, Instant.now());
        novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclo, TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2208, 3, 2), StatusAvaliacao.FINALIZADA, Instant.now());
        novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclo, TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2208, 3, 3), StatusAvaliacao.FINALIZADA, Instant.now());

        Page<Avaliacao> pagina = avaliacaoRepository.buscarHistorico(
                aluno.getId(), StatusAvaliacao.FINALIZADA, null, null, null, PageRequest.of(0, 2));

        assertEquals(3, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
        assertEquals(2, pagina.getContent().size());
    }

    @Test
    void buscarFinalizadasPorAnoETipoOrdenaPorCicloEFinalizadoEmDescEFiltraEscopo() {
        prepararCadastros(2209);
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Evolucao Ciclo"));
        List<Ciclo> ciclos = ciclosOrdenadosPorId();
        Instant agora = Instant.now();

        Avaliacao ciclo0Unica = novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclos.get(0), TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2209, 3, 1), StatusAvaliacao.FINALIZADA, agora.minus(Duration.ofDays(10)));
        Avaliacao ciclo1Antiga = novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclos.get(1), TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2209, 4, 1), StatusAvaliacao.FINALIZADA, agora.minus(Duration.ofDays(5)));
        Avaliacao ciclo1Recente = novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclos.get(1), TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2209, 5, 1), StatusAvaliacao.FINALIZADA, agora);
        // fora do escopo: cancelada no mesmo (aluno, ano, tipo, ciclo)
        novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclos.get(0), TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2209, 3, 5), StatusAvaliacao.CANCELADA, agora);
        // fora do escopo: tipo de leitura diferente
        novaAvaliacaoHistorico(
                aluno, anoLetivo, turma, ciclos.get(0), TipoLeituraCodigo.TEXTO_CURTO,
                LocalDate.of(2209, 3, 6), StatusAvaliacao.FINALIZADA, agora);

        List<Long> ids = avaliacaoRepository
                .buscarFinalizadasPorAnoETipo(
                        aluno.getId(), StatusAvaliacao.FINALIZADA, anoLetivo.getId(), TipoLeituraCodigo.PALAVRA)
                .stream()
                .map(Avaliacao::getId)
                .toList();

        assertEquals(List.of(ciclo0Unica.getId(), ciclo1Recente.getId(), ciclo1Antiga.getId()), ids);
    }

    @Test
    void buscarFinalizadasPorTipoOrdenaPorAnoLetivoDepoisCicloEFiltraEscopo() {
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Evolucao Anual"));
        AnoLetivo ano2210 = novoAnoLetivo(2210);
        AnoLetivo ano2211 = novoAnoLetivo(2211);
        Turma turma2210 = novaTurmaEm(ano2210, "2210");
        Turma turma2211 = novaTurmaEm(ano2211, "2211");
        List<Ciclo> ciclos = ciclosOrdenadosPorId();
        Instant agora = Instant.now();

        Avaliacao ano2210Ciclo0Recente = novaAvaliacaoHistorico(
                aluno, ano2210, turma2210, ciclos.get(0), TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2210, 3, 1), StatusAvaliacao.FINALIZADA, agora);
        // mesmo (ano, ciclo) que a anterior, finalizadoEm mais antigo (HIST-20):
        // prova que a query ordena por finalizadoEm desc dentro do grupo, não só por inserção.
        Avaliacao ano2210Ciclo0Antiga = novaAvaliacaoHistorico(
                aluno, ano2210, turma2210, ciclos.get(0), TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2210, 2, 1), StatusAvaliacao.FINALIZADA, agora.minus(Duration.ofDays(10)));
        Avaliacao ano2210Ciclo1 = novaAvaliacaoHistorico(
                aluno, ano2210, turma2210, ciclos.get(1), TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2210, 4, 1), StatusAvaliacao.FINALIZADA, agora);
        Avaliacao ano2211Ciclo0 = novaAvaliacaoHistorico(
                aluno, ano2211, turma2211, ciclos.get(0), TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2211, 3, 1), StatusAvaliacao.FINALIZADA, agora);
        // fora do escopo: tipo de leitura diferente
        novaAvaliacaoHistorico(
                aluno, ano2211, turma2211, ciclos.get(1), TipoLeituraCodigo.TEXTO_CURTO,
                LocalDate.of(2211, 4, 1), StatusAvaliacao.FINALIZADA, agora);
        // fora do escopo: nao finalizada
        novaAvaliacaoHistorico(
                aluno, ano2211, turma2211, ciclos.get(1), TipoLeituraCodigo.PALAVRA,
                LocalDate.of(2211, 4, 2), StatusAvaliacao.EM_ANDAMENTO, null);

        List<Long> ids = avaliacaoRepository
                .buscarFinalizadasPorTipo(aluno.getId(), StatusAvaliacao.FINALIZADA, TipoLeituraCodigo.PALAVRA)
                .stream()
                .map(Avaliacao::getId)
                .toList();

        assertEquals(
                List.of(ano2210Ciclo0Recente.getId(), ano2210Ciclo0Antiga.getId(), ano2210Ciclo1.getId(),
                        ano2211Ciclo0.getId()),
                ids);
    }
}
