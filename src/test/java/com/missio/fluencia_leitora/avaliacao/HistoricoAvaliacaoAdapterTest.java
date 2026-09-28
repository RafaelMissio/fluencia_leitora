package com.missio.fluencia_leitora.avaliacao;

import com.missio.fluencia_leitora.cadastros.aluno.HistoricoAvaliacaoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link HistoricoAvaliacaoAdapter} só delega para {@link
 * AvaliacaoRepository#existsByAlunoIdAndStatusNot}, sempre com {@code
 * StatusAvaliacao.CANCELADA} como status excluído - a distinção entre
 * "nenhuma avaliação", "só CANCELADA" e "com uma não-CANCELADA" é
 * responsabilidade da query do repositório, já coberta em {@code
 * AvaliacaoRepositoryIT} (T8). Este teste confirma a delegação (o valor
 * retornado e o argumento de status) e, por composição com qualquer
 * {@code *ControllerIT} do projeto que sobe o contexto completo, que o
 * {@code @Primary} resolve o bean sem ambiguidade com {@code
 * HistoricoAvaliacaoPortStub}.
 */
@ExtendWith(MockitoExtension.class)
class HistoricoAvaliacaoAdapterTest {

    private static final Long ALUNO_ID = 42L;

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    private HistoricoAvaliacaoPort port;

    @BeforeEach
    void setUp() {
        port = new HistoricoAvaliacaoAdapter(avaliacaoRepository);
    }

    @Test
    void delegaParaORepositorioExcluindoApenasCanceladaERetornaTrue() {
        when(avaliacaoRepository.existsByAlunoIdAndStatusNot(ALUNO_ID, StatusAvaliacao.CANCELADA)).thenReturn(true);

        assertTrue(port.existeAvaliacaoNaoCancelada(ALUNO_ID));
        verify(avaliacaoRepository).existsByAlunoIdAndStatusNot(ALUNO_ID, StatusAvaliacao.CANCELADA);
    }

    @Test
    void retornaFalseQuandoORepositorioNaoEncontraAvaliacaoNaoCancelada() {
        when(avaliacaoRepository.existsByAlunoIdAndStatusNot(ALUNO_ID, StatusAvaliacao.CANCELADA)).thenReturn(false);

        assertFalse(port.existeAvaliacaoNaoCancelada(ALUNO_ID));
    }

    @Test
    void passaOAlunoIdRecebidoSemAlterar() {
        Long outroAluno = 999L;
        when(avaliacaoRepository.existsByAlunoIdAndStatusNot(outroAluno, StatusAvaliacao.CANCELADA)).thenReturn(true);

        assertTrue(port.existeAvaliacaoNaoCancelada(outroAluno));
        verify(avaliacaoRepository).existsByAlunoIdAndStatusNot(outroAluno, StatusAvaliacao.CANCELADA);
    }
}
