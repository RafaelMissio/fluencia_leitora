package com.missio.fluencia_leitora.autenticacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RedefinicaoSenhaTokenRepository extends JpaRepository<RedefinicaoSenhaToken, Long> {

    Optional<RedefinicaoSenhaToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM RedefinicaoSenhaToken t WHERE t.usuarioId = :usuarioId AND t.usadoEm IS NULL")
    void apagarPendentes(@Param("usuarioId") Long usuarioId);
}
