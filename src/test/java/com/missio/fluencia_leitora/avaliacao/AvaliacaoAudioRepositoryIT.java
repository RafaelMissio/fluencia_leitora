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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T2 (historicoevolucao, HIST-02): {@code findAvaliacaoIdByAvaliacaoIdIn}
 * marca {@code temAudio} numa página de histórico - só traz os ids que
 * têm um {@link AvaliacaoAudio} associado, exercitado contra MySQL real.
 */
@Transactional
class AvaliacaoAudioRepositoryIT extends IntegrationTestBase {

    @Autowired
    private AvaliacaoAudioRepository avaliacaoAudioRepository;

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

    private Avaliacao novaAvaliacao(int ano, Aluno aluno) {
        AnoLetivo anoLetivo = anoLetivoRepository.save(new AnoLetivo(ano, LocalDate.of(ano, 2, 1), LocalDate.of(ano, 12, 15)));
        Turma turma = turmaRepository.save(new Turma("Turma Audio " + ano, 2, anoLetivo, null));
        Ciclo ciclo = cicloRepository.findAll().get(0);
        Avaliacao avaliacao = new Avaliacao(
                aluno, null, null, turma, turma.getNome(), 2, anoLetivo, ciclo,
                TipoLeituraCodigo.PALAVRA, LocalDate.of(ano, 3, 1), 60);
        avaliacao.adicionarPalavra("gato", null);
        avaliacao.setStatus(StatusAvaliacao.FINALIZADA);
        return avaliacaoRepository.saveAndFlush(avaliacao);
    }

    @Test
    void findAvaliacaoIdByAvaliacaoIdInRetornaSoOsIdsComAudio() {
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Audio Presente"));
        Avaliacao comAudio = novaAvaliacao(3200, aluno);
        Avaliacao semAudio = novaAvaliacao(3201, aluno);
        avaliacaoAudioRepository.save(new AvaliacaoAudio(comAudio, "ref-audio.wav", "audio/wav", 1024L));

        List<Long> ids = avaliacaoAudioRepository.findAvaliacaoIdByAvaliacaoIdIn(
                List.of(comAudio.getId(), semAudio.getId()));

        assertEquals(List.of(comAudio.getId()), ids);
    }

    @Test
    void findAvaliacaoIdByAvaliacaoIdInNaoRetornaIdsSemAudio() {
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Audio Ausente"));
        Avaliacao semAudio = novaAvaliacao(3202, aluno);

        List<Long> ids = avaliacaoAudioRepository.findAvaliacaoIdByAvaliacaoIdIn(List.of(semAudio.getId()));

        assertTrue(ids.isEmpty());
    }

    @Test
    void findAvaliacaoIdByAvaliacaoIdInComListaVaziaRetornaListaVazia() {
        List<Long> ids = avaliacaoAudioRepository.findAvaliacaoIdByAvaliacaoIdIn(List.of());

        assertTrue(ids.isEmpty());
    }
}
