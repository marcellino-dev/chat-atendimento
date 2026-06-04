package com.empresa.chat.service.atendimento;

import com.empresa.chat.domain.enums.Role;
import com.empresa.chat.domain.enums.StatusAtendente;
import com.empresa.chat.domain.model.Atendente;
import com.empresa.chat.domain.model.User;
import com.empresa.chat.dto.response.AtendenteResponse;
import com.empresa.chat.exception.BusinessException;
import com.empresa.chat.exception.NotFoundException;
import com.empresa.chat.repository.AtendenteRepository;
import com.empresa.chat.repository.SetorRepository;
import com.empresa.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AtendenteService {

    private final AtendenteRepository atendenteRepository;
    private final UserRepository userRepository;
    private final SetorRepository setorRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AtendenteResponse criar(String nome, String email, String senha, Long setorId) {
        if (userRepository.existsByEmail(email))
            throw new BusinessException("Email já cadastrado: " + email);

        var setor = setorId != null ? setorRepository.findById(setorId).orElse(null) : null;

        var user = User.builder()
            .nome(nome).email(email)
            .senha(passwordEncoder.encode(senha))
            .role(Role.ATENDENTE).build();
        userRepository.save(user);

        var atendente = Atendente.builder()
            .user(user).setor(setor)
            .status(StatusAtendente.OFFLINE).build();
        return toResponse(atendenteRepository.save(atendente));
    }

    @Transactional(readOnly = true)
    public List<AtendenteResponse> listar() {
        return atendenteRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AtendenteResponse atualizarStatus(Long id, StatusAtendente status) {
        var atendente = atendenteRepository.findById(id)
            .orElseThrow(() -> NotFoundException.of("Atendente", id));
        atendente.setStatus(status);
        return toResponse(atendenteRepository.save(atendente));
    }

    private AtendenteResponse toResponse(Atendente a) {
        return new AtendenteResponse(
            a.getId(), a.getUser().getId(), a.getUser().getNome(), a.getUser().getEmail(),
            a.getStatus().name(), a.getSetor() != null ? a.getSetor().getNome() : null,
            a.getMaxSimultaneous()
        );
    }
}
