package com.missio.fluencia_leitora.cadastros.aluno;

import com.missio.fluencia_leitora.cadastros.aluno.AlunoService.AlunoBusca;
import com.missio.fluencia_leitora.cadastros.aluno.AlunoService.AlunoComMatricula;
import com.missio.fluencia_leitora.cadastros.aluno.dto.AlunoBuscaItemResponse;
import com.missio.fluencia_leitora.cadastros.aluno.dto.AlunoResponse;
import com.missio.fluencia_leitora.cadastros.aluno.dto.AtualizarNomeAlunoRequest;
import com.missio.fluencia_leitora.cadastros.aluno.dto.CriarAlunoRequest;
import com.missio.fluencia_leitora.cadastros.aluno.dto.CriarAlunoResponse;
import com.missio.fluencia_leitora.common.security.ContextoUsuarioPort;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CAD-11/CAD-15/CAD-16/CAD-19/CAD-20: cadastro de aluno, busca por nome e bloqueio de nome. */
@RestController
@RequestMapping("/api/v1/alunos")
public class AlunoController {

    private static final int TAMANHO_PAGINA = 20;

    private final AlunoService alunoService;
    private final ContextoUsuarioPort contextoUsuarioPort;

    public AlunoController(AlunoService alunoService, ContextoUsuarioPort contextoUsuarioPort) {
        this.alunoService = alunoService;
        this.contextoUsuarioPort = contextoUsuarioPort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CriarAlunoResponse criar(@Valid @RequestBody CriarAlunoRequest request) {
        AlunoComMatricula criado = alunoService.criarComMatricula(request.nome(), request.turmaId());
        return CriarAlunoResponse.from(criado);
    }

    @GetMapping
    public Page<AlunoBuscaItemResponse> buscar(
            @RequestParam String nome, @RequestParam(defaultValue = "0") int page) {
        Page<AlunoBusca> resultado =
                alunoService.buscar(nome, PageRequest.of(page, TAMANHO_PAGINA), contextoUsuarioPort);
        return resultado.map(AlunoBuscaItemResponse::from);
    }

    @PutMapping("/{id}")
    public AlunoResponse atualizarNome(@PathVariable Long id, @Valid @RequestBody AtualizarNomeAlunoRequest request) {
        return AlunoResponse.from(alunoService.atualizarNome(id, request.nome()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        alunoService.inativar(id);
    }
}
