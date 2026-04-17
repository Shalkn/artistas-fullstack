package com.artistas.api.bootstrap;

import com.artistas.api.domain.AppUser;
import com.artistas.api.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Garante um usuário administrador quando o banco está vazio, para permitir o primeiro login
 * sem migração manual de dados sensíveis. Credenciais devem constar no README do repositório.
 */
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Executado uma vez na subida da aplicação. Regra de negócio: só cria o admin se não existir
     * nenhum usuário — evita sobrescrever contas em ambientes já populados.
     *
     * @param args argumentos de inicialização do Spring Boot (não utilizados aqui)
     */
    @Override
    public void run(ApplicationArguments args) {
        if (appUserRepository.count() > 0) {
            return;
        }
        appUserRepository.save(AppUser.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .enabled(true)
                .build());
    }
}
