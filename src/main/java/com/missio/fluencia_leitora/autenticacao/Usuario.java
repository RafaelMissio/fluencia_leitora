package com.missio.fluencia_leitora.autenticacao;

import com.missio.fluencia_leitora.common.security.Perfil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Locale;

/**
 * Credenciais + perfil de acesso. O e-mail é sempre gravado em minúsculas,
 * o que torna a constraint única {@code uk_usuario_email} case-insensitive
 * (AUTH-12). A senha só existe aqui como hash BCrypt (AUTH-05).
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Perfil perfil;

    @Column(name = "professor_id")
    private Long professorId;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "tentativas_falhas", nullable = false)
    private int tentativasFalhas;

    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Usuario() {
    }

    public Usuario(String email, String senhaHash, Perfil perfil, Long professorId) {
        this.email = email.toLowerCase(Locale.ROOT);
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.professorId = professorId;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public Long getProfessorId() {
        return professorId;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public int getTentativasFalhas() {
        return tentativasFalhas;
    }

    public Instant getBloqueadoAte() {
        return bloqueadoAte;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
