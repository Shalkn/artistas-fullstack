package com.artistas.api.repository;

import com.artistas.api.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Usuários de API; login resolve por {@link #findByUsername(String)}. */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
}
