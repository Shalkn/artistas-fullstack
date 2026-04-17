package com.artistas.api.repository;

import com.artistas.api.domain.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** CRUD e busca por nome para listagem paginada. */
public interface ArtistRepository extends JpaRepository<Artist, Long> {

    /**
     * Busca case-insensitive por substring. Não passar {@code name} nulo — use {@link #findAll(Pageable)}.
     */
    Page<Artist> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
