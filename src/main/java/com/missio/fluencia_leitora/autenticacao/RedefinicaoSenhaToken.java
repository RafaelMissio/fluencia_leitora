package com.missio.fluencia_leitora.autenticacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Token de uso único para redefinir a senha; só o hash SHA-256 é persistido. */
@Entity
@Table(name = "redefinicao_senha_token")
public class RedefinicaoSenhaToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    protected RedefinicaoSenhaToken() {
    }

    public RedefinicaoSenhaToken(Long usuarioId, String tokenHash, Instant expiraEm) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.expiraEm = expiraEm;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public boolean estaValido(Instant agora) {
        return usadoEm == null && expiraEm.isAfter(agora);
    }

    public void marcarUsado(Instant agora) {
        this.usadoEm = agora;
    }
}
