package com.missio.fluencia_leitora.cadastros.aluno;

import com.missio.fluencia_leitora.cadastros.aluno.AlunoService.AlunoBusca;
import com.missio.fluencia_leitora.cadastros.aluno.AlunoService.AlunoComMatricula;
import com.missio.fluencia_leitora.cadastros.aluno.dto.AlterarSituacaoAlunoRequest;
import com.missio.fluencia_leitora.cadastros.aluno.dto.AlunoBuscaItemResponse;
import com.missio.fluencia_leitora.cadastros.aluno.dto.AlunoResponse;
import com.missio.fluencia_leitora.cadastros.aluno.dto.AtualizarNomeAlunoRequest;
import com.missio.fluencia_leitora.cadastros.aluno.dto.CriarAlunoRequest;
import com.missio.fluencia_leitora.cadastros.aluno.dto.CriarAlunoResponse;
import com.missio.fluencia_leitora.common.security.ContextoUsuarioPort;
import com.missio.fluencia_leitora.common.security.PertencimentoProfessorGuard;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CAD-11/CAD-15/CAD-16/CAD-19/CAD-20: cadastro de aluno, busca por nome e
 * bloqueio de nome. Escrita restrita ao COORDENADOR (AUTH-07); a leitura por
 * id devolve 404 ao PROFESSOR que não é o dono do aluno no ano ATIVO (AUTH-09).
 */
@RestController
@RequestMapping("/api/v1/alunos")
public class AlunoController {

    private static final int TAMANHO_PAGINA = 20;

    private final AlunoService alunoService;
    private final ContextoUsuarioPort contextoUsuarioPort;
    private final PertencimentoProfessorGuard pertencimentoProfessorGuard;

    public AlunoController(
            AlunoService alunoService,
            ContextoUsuarioPort contextoUsuarioPort,
            PertencimentoProfessorGuard pertencimentoProfessorGuard) {
        this.alunoService = alunoService;
        this.contextoUsuarioPort = contextoUsuarioPort;
        this.pertencimentoProfessorGuard = pertencimentoProfessorGuard;
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDENADOR')")
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

    @GetMapping("/{id}")
    public AlunoBuscaItemResponse buscarPorId(@PathVariable Long id) {
        AlunoBusca alunoBusca = alunoService.buscarPorId(id);
        Matricula matriculaAtiva = alunoBusca.matriculaAtiva();
        pertencimentoProfessorGuard.verificar(
                matriculaAtiva == null || matriculaAtiva.getProfessor() == null
                        ? null
                        : matriculaAtiva.getProfessor().getId());
        return AlunoBuscaItemResponse.from(alunoBusca);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDENADOR')")
    public AlunoResponse atualizarNome(@PathVariable Long id, @Valid @RequestBody AtualizarNomeAlunoRequest request) {
        return AlunoResponse.from(alunoService.atualizarNome(id, request.nome()));
    }

    @PatchMapping("/{id}/situacao")
    @PreAuthorize("hasRole('COORDENADOR')")
    public AlunoResponse alterarSituacao(
            @PathVariable Long id, @Valid @RequestBody AlterarSituacaoAlunoRequest request) {
        return AlunoResponse.from(alunoService.alterarAtivo(id, request.ativo()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDENADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        alunoService.inativar(id);
    }
}
