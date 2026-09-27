package com.missio.fluencia_leitora.cadastros.turma;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TurmaRepository extends JpaRepository<Turma, Long> {

    boolean existsByNomeIgnoreCaseAndAnoLetivoId(String nome, Long anoLetivoId);

    List<Turma> findByProfessorIdAndAtivoTrue(Long professorId);
}
