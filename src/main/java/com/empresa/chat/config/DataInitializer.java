package com.empresa.chat.config;

import com.empresa.chat.domain.enums.Role;
import com.empresa.chat.domain.enums.StatusAtendente;
import com.empresa.chat.domain.model.Atendente;
import com.empresa.chat.domain.model.Setor;
import com.empresa.chat.domain.model.User;
import com.empresa.chat.repository.AtendenteRepository;
import com.empresa.chat.repository.SetorRepository;
import com.empresa.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final SetorRepository setorRepository;
    private final UserRepository userRepository;
    private final AtendenteRepository atendenteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Inicializando dados de teste...");

        criarSetores();
        criarAtendentes();

        log.info("Dados de teste inicializados!");
    }

    private void criarSetores() {
        String[] nomesSetores = {"Financeiro", "Suporte Técnico", "Comercial", "Outros", "Certificados"};

        for (int i = 0; i < nomesSetores.length; i++) {
            String nome = nomesSetores[i];
            if (setorRepository.findByNomeIgnoreCase(nome).isEmpty()) {
                Setor setor = Setor.builder()
                        .nome(nome)
                        .descricao("Setor de " + nome)
                        .ativo(true)
                        .build();
                setorRepository.save(setor);
                log.info("Setor criado: {}", nome);
            } else {
                log.info("Setor já existe: {}", nome);
            }
        }
    }

    private void criarAtendentes() {
        // Busca os setores pelo nome
        Setor financeiro = setorRepository.findByNomeIgnoreCase("Financeiro").orElse(null);
        Setor suporte = setorRepository.findByNomeIgnoreCase("Suporte Técnico").orElse(null);
        Setor comercial = setorRepository.findByNomeIgnoreCase("Comercial").orElse(null);
        Setor certificados = setorRepository.findByNomeIgnoreCase("Certificados").orElse(null);

        // Atendente 1 - Financeiro
        criarAtendenteSeNaoExiste(
                "atendente1@chat.com",
                "123456",
                "Carlos Silva",
                financeiro
        );

        // Atendente 2 - Financeiro
        criarAtendenteSeNaoExiste(
                "atendente2@chat.com",
                "123456",
                "Ana Oliveira",
                financeiro
        );

        // Atendente 3 - Suporte Técnico
        criarAtendenteSeNaoExiste(
                "suporte1@chat.com",
                "123456",
                "Roberto Santos",
                suporte
        );

        // Atendente 4 - Comercial
        criarAtendenteSeNaoExiste(
                "comercial1@chat.com",
                "123456",
                "Fernanda Lima",
                comercial
        );

        // Atendente 5 - Certificados
        criarAtendenteSeNaoExiste(
                "certificados1@chat.com",
                "123456",
                "Paulo Ricardo",
                certificados
        );

        // Admin
        criarAdminSeNaoExiste(
                "admin@chat.com",
                "123456",
                "Administrador"
        );
    }

    private void criarAtendenteSeNaoExiste(String email, String senha, String nome, Setor setor) {
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Atendente {} já existe", email);
            return;
        }

        // Cria usuário
        User user = User.builder()
                .nome(nome)
                .email(email)
                .senha(passwordEncoder.encode(senha))
                .role(Role.ATENDENTE)
                .ativo(true)
                .build();
        User savedUser = userRepository.save(user);

        // Cria atendente (relacionamento com User)
        Atendente atendente = Atendente.builder()
                .user(savedUser)
                .setor(setor)
                .status(StatusAtendente.ONLINE)
                .maxSimultaneous(5)
                .build();

        atendenteRepository.save(atendente);

        log.info("Atendente criado: {} - {} - ONLINE", nome, email);
    }

    private void criarAdminSeNaoExiste(String email, String senha, String nome) {
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Admin {} já existe", email);
            return;
        }

        User user = User.builder()
                .nome(nome)
                .email(email)
                .senha(passwordEncoder.encode(senha))
                .role(Role.ADMIN)
                .ativo(true)
                .build();
        userRepository.save(user);

        log.info("Admin criado: {} - {}", nome, email);
    }
}