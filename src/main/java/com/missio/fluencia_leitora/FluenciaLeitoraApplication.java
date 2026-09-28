package com.missio.fluencia_leitora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @EnableScheduling}: primeiro job agendado do projeto -
 * {@code AvaliacaoFinalizacaoScheduler} (AVA-17, design.md Risks & Concerns).
 */
@SpringBootApplication
@EnableScheduling
public class FluenciaLeitoraApplication {

	public static void main(String[] args) {
		SpringApplication.run(FluenciaLeitoraApplication.class, args);
	}

}
