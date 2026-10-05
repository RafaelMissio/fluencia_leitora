package com.missio.fluencia_leitora.cadastros.aluno;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    boolean existsByAlunoIdAndAnoLetivoId(Long alunoId, Long anoLetivoId);

    List<Matricula> findByAlunoId(Long alunoId);

    List<Matricula> findByTurmaIdOrderByAlunoNomeAsc(Long turmaId);

    Optional<Matricula> findByAlunoIdAndAnoLetivoId(Long alunoId, Long anoLetivoId);
}
