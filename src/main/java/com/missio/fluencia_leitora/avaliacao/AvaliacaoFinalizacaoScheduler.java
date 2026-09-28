package com.missio.fluencia_leitora.avaliacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job de hora em hora que finaliza avaliações {@code EM_ANDAMENTO} paradas
 * há mais de 24h (AVA-17, spec.md Edge Cases; design.md, Components e
 * Risks & Concerns - primeiro {@code @Scheduled} do projeto).
 */
@Component
public class AvaliacaoFinalizacaoScheduler {

    private static final Logger log = LoggerFactory.getLogger(AvaliacaoFinalizacaoScheduler.class);

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoFinalizacaoScheduler(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void finalizarInativas() {
        int quantidade = avaliacaoService.finalizarInativas();
        if (quantidade > 0) {
            log.info("Finalização automática por inatividade: {} avaliações finalizadas", quantidade);
        }
    }
}
