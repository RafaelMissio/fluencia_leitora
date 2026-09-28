package com.missio.fluencia_leitora.regrasclassificacao;

import com.missio.fluencia_leitora.autenticacao.Usuario;
import com.missio.fluencia_leitora.autenticacao.UsuarioRepository;
import com.missio.fluencia_leitora.common.security.Perfil;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * `RegraClassificacaoRepository` contra MySQL real: consultas por série
 * (T6) e o lock pessimista de `buscarAtivasParaAtualizarComLock`, provado
 * com duas transações concorrentes de verdade (design.md, Risks &
 * Concerns - não basta uma chamada dupla no mesmo thread).
 */
@Transactional
class RegraClassificacaoRepositoryIT extends IntegrationTestBase {

    @Autowired
    private RegraClassificacaoRepository regraClassificacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** `alterado_por` referencia `usuario(id)` (FK) - precisa de um usuário real gravado. */
    private Long usuarioDeTesteId() {
        return usuarioRepository
                .save(new Usuario("regra-classificacao-it@escola.com", "hash-nao-usado", Perfil.COORDENADOR, null))
                .getId();
    }

    @Test
    void findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAscRetornaFaixasDaSerieEmOrdemCrescente() {
        // V7 seed: série 4 tem 6 faixas ativas com mínimos 0, 5, 8, 10, 12, 31.
        List<RegraClassificacao> faixas =
                regraClassificacaoRepository.findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc(4);

        assertEquals(6, faixas.size());
        assertEquals(List.of(0, 5, 8, 10, 12, 31), faixas.stream().map(RegraClassificacao::getQuantidadeMinimaAcertos).toList());
        assertTrue(faixas.stream().allMatch(RegraClassificacao::isAtivo));
    }

    @Test
    void findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAscNaoRetornaFaixaInativa() {
        RegraClassificacao faixaInativa = new RegraClassificacao(3, 999, null, Fase.LEITOR_FLUENTE, null);
        faixaInativa.inativar(usuarioDeTesteId(), Instant.now());
        regraClassificacaoRepository.save(faixaInativa);

        List<RegraClassificacao> faixas =
                regraClassificacaoRepository.findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc(3);

        assertEquals(6, faixas.size());
        assertTrue(faixas.stream().noneMatch(f -> f.getQuantidadeMinimaAcertos() == 999));
    }

    @Test
    void buscarHistoricoPorSerieRetornaGrupoCorrentePrimeiroDepoisAlteradoEmDesc() {
        Long usuarioId = usuarioDeTesteId();

        RegraClassificacao grupoAntigo = new RegraClassificacao(2, 900, null, Fase.LEITOR_FLUENTE, null);
        grupoAntigo.inativar(usuarioId, Instant.now().minus(2, ChronoUnit.DAYS));
        regraClassificacaoRepository.save(grupoAntigo);

        RegraClassificacao grupoRecente = new RegraClassificacao(2, 901, null, Fase.LEITOR_FLUENTE, null);
        grupoRecente.inativar(usuarioId, Instant.now().minus(1, ChronoUnit.DAYS));
        regraClassificacaoRepository.save(grupoRecente);

        List<RegraClassificacao> historico = regraClassificacaoRepository.buscarHistoricoPorSerie(2);

        // V7 seed: série 2 tem 6 faixas ativas (grupo corrente, alteradoEm=null) + os 2 grupos históricos acima.
        assertEquals(8, historico.size());
        // Os 6 primeiros são o grupo corrente (alteradoEm=null).
        assertTrue(historico.subList(0, 6).stream().allMatch(f -> f.getAlteradoEm() == null));
        // Os 2 últimos são o histórico, do mais recente para o mais antigo.
        assertEquals(grupoRecente.getId(), historico.get(6).getId());
        assertEquals(grupoAntigo.getId(), historico.get(7).getId());
    }

    @Test
    void buscarAtivasParaAtualizarComLockSerializaDuasTransacoesConcorrentesNaMesmaSerie() throws Exception {
        TransactionTemplate txA = new TransactionTemplate(transactionManager);
        TransactionTemplate txB = new TransactionTemplate(transactionManager);

        CountDownLatch txAAdquiriuLock = new CountDownLatch(1);
        CountDownLatch liberarTxA = new CountDownLatch(1);
        // Os dois instantes são registrados de DENTRO da própria thread de cada transação, não
        // pela thread principal depois de um `Future.get()` sequencial: medir na thread principal
        // provaria a ordem "por construção" (nanoTime é monotônico por thread), sem realmente medir
        // se txB ficou bloqueada esperando o lock de txA.
        AtomicLong instantePoucoAntesDoCommitTxA = new AtomicLong(-1);
        AtomicLong instanteEmQueOSelectDeTxBDesbloqueou = new AtomicLong(-1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var futureA = executor.submit(() -> txA.executeWithoutResult(status -> {
                regraClassificacaoRepository.buscarAtivasParaAtualizarComLock(1);
                txAAdquiriuLock.countDown();
                try {
                    // segura a transação (e o lock) aberta até o teste mandar liberar
                    assertTrue(liberarTxA.await(5, TimeUnit.SECONDS), "txA não foi liberada a tempo");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // `executeWithoutResult` comita assim que este lambda retornar - o commit real
                // acontece logo depois deste instante, nunca antes.
                instantePoucoAntesDoCommitTxA.set(System.nanoTime());
            }));

            assertTrue(txAAdquiriuLock.await(5, TimeUnit.SECONDS), "txA não adquiriu o lock a tempo");

            var futureB = executor.submit(() -> txB.executeWithoutResult(status -> {
                regraClassificacaoRepository.buscarAtivasParaAtualizarComLock(1);
                instanteEmQueOSelectDeTxBDesbloqueou.set(System.nanoTime());
            }));

            // dá tempo de txB tentar o SELECT ... FOR UPDATE e ficar bloqueada esperando txA
            Thread.sleep(300);
            liberarTxA.countDown();

            futureA.get(5, TimeUnit.SECONDS);
            futureB.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }

        assertTrue(instanteEmQueOSelectDeTxBDesbloqueou.get() >= instantePoucoAntesDoCommitTxA.get(),
                "txB só deveria destravar o SELECT ... FOR UPDATE depois que txA liberasse o lock "
                        + "(lock pessimista provado - sem o lock, txB destravaria quase que "
                        + "imediatamente, bem antes desse instante)");
    }
}
