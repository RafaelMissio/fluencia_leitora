package com.missio.fluencia_leitora.cadastros.professor;

import com.missio.fluencia_leitora.cadastros.professor.dto.CriarProfessorRequest;
import com.missio.fluencia_leitora.cadastros.professor.dto.ProfessorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CAD-07/CAD-08/CAD-09/CAD-19: CRUD de professor com consulta de turmas ativas. */
@RestController
@RequestMapping("/api/v1/professores")
public class ProfessorController {

    private final ProfessorService professorService;

    public ProfessorController(ProfessorService professorService) {
        this.professorService = professorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessorResponse criar(@Valid @RequestBody CriarProfessorRequest request) {
        return ProfessorResponse.from(professorService.criar(request.nome()));
    }

    @GetMapping("/{id}")
    public ProfessorResponse buscar(@PathVariable Long id) {
        return ProfessorResponse.from(professorService.buscarComTurmas(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        professorService.inativar(id);
    }
}
