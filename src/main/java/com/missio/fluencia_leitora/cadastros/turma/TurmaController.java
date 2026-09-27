package com.missio.fluencia_leitora.cadastros.turma;

import com.missio.fluencia_leitora.cadastros.turma.dto.AtualizarProfessorRequest;
import com.missio.fluencia_leitora.cadastros.turma.dto.CriarTurmaRequest;
import com.missio.fluencia_leitora.cadastros.turma.dto.TurmaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CAD-07/CAD-10/CAD-19: CRUD de turma com vínculo a professor e ano letivo. */
@RestController
@RequestMapping("/api/v1/turmas")
public class TurmaController {

    private final TurmaService turmaService;

    public TurmaController(TurmaService turmaService) {
        this.turmaService = turmaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TurmaResponse criar(@Valid @RequestBody CriarTurmaRequest request) {
        Turma criada = turmaService.criar(request.nome(), request.serie(), request.anoLetivoId(), request.professorId());
        return TurmaResponse.from(criada);
    }

    @PutMapping("/{id}")
    public TurmaResponse atualizarProfessor(@PathVariable Long id, @Valid @RequestBody AtualizarProfessorRequest request) {
        return TurmaResponse.from(turmaService.atualizarProfessor(id, request.professorId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        turmaService.inativar(id);
    }
}
