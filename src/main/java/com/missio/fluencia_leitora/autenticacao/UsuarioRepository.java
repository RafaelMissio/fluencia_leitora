package com.missio.fluencia_leitora.autenticacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    /**
     * AUTH-03: incrementa o contador de falhas e, só quando o novo valor
     * atinge {@code limite}, grava {@code bloqueadoAte} - numa única
     * instrução UPDATE (sem ler-modificar-gravar).
     *
     * <p>{@code bloqueado_ate} é atribuído antes de {@code tentativas_falhas}
     * porque o MySQL avalia as atribuições do SET da esquerda para a direita:
     * na ordem inversa, o CASE já enxergaria o contador incrementado.
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = "UPDATE usuario SET "
                    + "bloqueado_ate = CASE WHEN tentativas_falhas + 1 >= :limite "
                    + "THEN :bloqueadoAte ELSE bloqueado_ate END, "
                    + "tentativas_falhas = tentativas_falhas + 1 "
                    + "WHERE id = :id",
            nativeQuery = true)
    void registrarFalha(
            @Param("id") Long id, @Param("bloqueadoAte") Instant bloqueadoAte, @Param("limite") int limite);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Usuario u SET u.tentativasFalhas = 0, u.bloqueadoAte = null WHERE u.id = :id")
    void zerarFalhas(@Param("id") Long id);
}
