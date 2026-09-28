package com.missio.fluencia_leitora.avaliacao;

import com.missio.fluencia_leitora.cadastros.aluno.HistoricoAvaliacaoPort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Implementação real de {@link HistoricoAvaliacaoPort} (design.md,
 * Components) - substitui o {@code HistoricoAvaliacaoPortStub} provisório
 * de {@code cadastros-base} como bean resolvido por padrão, agora que a
 * tabela {@code avaliacao} existe. {@code @Primary} resolve a ambiguidade
 * entre os dois beans sem precisar mudar {@code cadastros-base}; o stub
 * continua no código, apenas deixa de ser o único candidato.
 */
@Primary
@Component
public class HistoricoAvaliacaoAdapter implements HistoricoAvaliacaoPort {

    private final AvaliacaoRepository avaliacaoRepository;

    public HistoricoAvaliacaoAdapter(AvaliacaoRepository avaliacaoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
    }

    @Override
    public boolean existeAvaliacaoNaoCancelada(Long alunoId) {
        return avaliacaoRepository.existsByAlunoIdAndStatusNot(alunoId, StatusAvaliacao.CANCELADA);
    }
}
