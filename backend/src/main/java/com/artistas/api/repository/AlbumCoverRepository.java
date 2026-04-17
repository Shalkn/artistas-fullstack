package com.artistas.api.repository;

import com.artistas.api.domain.AlbumCover;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Capas por álbum ordenadas por {@code sortOrder} crescente. */
public interface AlbumCoverRepository extends JpaRepository<AlbumCover, Long> {
    List<AlbumCover> findByAlbumIdOrderBySortOrderAsc(Long albumId);

    long countByAlbumId(Long albumId);
}
