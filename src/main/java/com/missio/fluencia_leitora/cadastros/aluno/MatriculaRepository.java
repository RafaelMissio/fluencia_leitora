package com.missio.fluencia_leitora.cadastros.aluno;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    boolean existsByAlunoIdAndAnoLetivoId(Long alunoId, Long anoLetivoId);

    List<Matricula> findByAlunoId(Long alunoId);
}
