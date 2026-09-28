package com.missio.fluencia_leitora.cadastros.aluno;

import com.missio.fluencia_leitora.cadastros.aluno.dto.AtualizarMatriculaRequest;
import com.missio.fluencia_leitora.cadastros.aluno.dto.MatriculaResponse;
import com.missio.fluencia_leitora.cadastros.aluno.dto.NovaMatriculaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CAD-12/CAD-13/CAD-14/CAD-17: nova matrícula em outro ano letivo e
 * atualização de professor/turma/ano finalizado. Restrito ao COORDENADOR
 * (AUTH-07).
 */
@RestController
@RequestMapping("/api/v1")
public class MatriculaController {

    private final MatriculaService matriculaService;

    public MatriculaController(MatriculaService matriculaService) {
        this.matriculaService = matriculaService;
    }

    @PostMapping("/alunos/{alunoId}/matriculas")
    @PreAuthorize("hasRole('COORDENADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public MatriculaResponse matricular(@PathVariable Long alunoId, @Valid @RequestBody NovaMatriculaRequest request) {
        return MatriculaResponse.from(matriculaService.matricular(alunoId, request.turmaId()));
    }

    @PatchMapping("/matriculas/{id}")
    @PreAuthorize("hasRole('COORDENADOR')")
    public MatriculaResponse atualizar(@PathVariable Long id, @RequestBody AtualizarMatriculaRequest request) {
        return MatriculaResponse.from(
                matriculaService.atualizar(id, request.professorId(), request.turmaId(), request.anoFinalizado()));
    }
}
