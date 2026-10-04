package com.missio.fluencia_leitora.autenticacao;

import com.missio.fluencia_leitora.autenticacao.dto.AlterarSenhaRequest;
import com.missio.fluencia_leitora.autenticacao.dto.CriarUsuarioRequest;
import com.missio.fluencia_leitora.autenticacao.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** AUTH-11/AUTH-12/AUTH-13: gestão de usuários, restrita ao COORDENADOR (AUTH-07). */
@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('COORDENADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar().stream().map(UsuarioResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody CriarUsuarioRequest request) {
        return UsuarioResponse.from(
                usuarioService.criar(request.email(), request.senha(), request.perfil(), request.professorId()));
    }

    @PutMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarSenha(@PathVariable Long id, @Valid @RequestBody AlterarSenhaRequest request) {
        usuarioService.alterarSenha(id, request.senha());
    }
}
