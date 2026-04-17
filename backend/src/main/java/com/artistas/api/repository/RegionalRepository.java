package com.artistas.api.repository;

import com.artistas.api.domain.Regional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Regionais sincronizadas da API externa; updates em massa marcam {@code ativo=false} sem apagar histórico.
 */
public interface RegionalRepository extends JpaRepository<Regional, Long> {

    @Query("""
            SELECT r FROM Regional r
            WHERE r.codigoExterno = :codigo AND r.ativo = true
            """)
    Optional<Regional> findActiveByCodigoExterno(@Param("codigo") Integer codigoExterno);

    @Query("""
            SELECT r FROM Regional r WHERE r.ativo = true
            """)
    List<Regional> findAllActive();

    @Modifying
    @Query("UPDATE Regional r SET r.ativo = false WHERE r.ativo = true AND r.codigoExterno NOT IN :codigos")
    int deactivateActiveNotInCodigos(@Param("codigos") List<Integer> codigos);

    @Modifying
    @Query("UPDATE Regional r SET r.ativo = false WHERE r.ativo = true")
    int deactivateAllActive();

    @Modifying
    @Query("UPDATE Regional r SET r.ativo = false WHERE r.id = :id")
    void deactivateById(@Param("id") Long id);
}
