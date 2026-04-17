package com.artistas.api.repository;

import com.artistas.api.domain.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Persistência de {@link com.artistas.api.domain.Album}; busca nativa para filtros seguros no PostgreSQL. */
public interface AlbumRepository extends JpaRepository<Album, Long> {

    List<Album> findByArtistIdOrderByTitleAsc(Long artistId);

    /**
     * SQL nativo (PostgreSQL): ILIKE evita lower(coluna), que falha se {@code title} estiver como bytea no banco.
     * Ordenação fixa em {@code title}; o {@link Pageable} só pagina (sem Sort duplicado).
     */
    @Query(
            value = """
                    SELECT al.id, al.artist_id, al.title, al.created_at, al.updated_at
                    FROM album al
                    WHERE (:artistId IS NULL OR al.artist_id = :artistId)
                    AND (
                        :title IS NULL
                        OR al.title ILIKE ('%' || CAST(:title AS VARCHAR) || '%')
                    )
                    ORDER BY al.title ASC
                    """,
            countQuery = """
                    SELECT count(*)
                    FROM album al
                    WHERE (:artistId IS NULL OR al.artist_id = :artistId)
                    AND (
                        :title IS NULL
                        OR al.title ILIKE ('%' || CAST(:title AS VARCHAR) || '%')
                    )
                    """,
            nativeQuery = true)
    Page<Album> search(
            @Param("artistId") Long artistId,
            @Param("title") String title,
            Pageable pageable);

    long countByArtistId(Long artistId);

    @Query("SELECT al.artist.id, COUNT(al) FROM Album al WHERE al.artist.id IN :ids GROUP BY al.artist.id")
    List<Object[]> countAlbumsByArtistIds(@Param("ids") List<Long> ids);
}
