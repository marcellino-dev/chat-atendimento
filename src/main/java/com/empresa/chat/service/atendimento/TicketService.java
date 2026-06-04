package com.empresa.chat.service.atendimento;

import com.empresa.chat.domain.enums.*;
import com.empresa.chat.domain.model.*;
import com.empresa.chat.dto.request.TicketRequest;
import com.empresa.chat.dto.response.TicketResponse;
import com.empresa.chat.exception.BusinessException;
import com.empresa.chat.exception.NotFoundException;
import com.empresa.chat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ConversaRepository conversaRepository;
    private final SetorRepository setorRepository;
    private final AtendenteRepository atendenteRepository;
    private final UserRepository userRepository;

    @Transactional
    public TicketResponse criarTicket(TicketRequest req) {
        var setor = setorRepository.findById(req.setorId())
            .orElseThrow(() -> NotFoundException.of("Setor", req.setorId()));

        var conversa = Conversa.builder()
            .clienteNome(req.clienteNome())
            .clienteTel(req.clienteTel())
            .canal(req.canal() != null ? req.canal() : CanalOrigem.WEB)
            .setor(setor)
            .status(StatusConversa.AGUARDANDO)
            .build();
        conversaRepository.save(conversa);

        var ticket = Ticket.builder()
            .conversa(conversa)
            .setor(setor)
            .status(StatusTicket.ABERTO)
            .prioridade(Ticket.Prioridade.NORMAL)
            .slaLimite(LocalDateTime.now().plusHours(4))
            .build();
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse assumirTicket(Long ticketId) {
        var ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> NotFoundException.of("Ticket", ticketId));

        if (ticket.getStatus() == StatusTicket.EM_ATENDIMENTO)
            throw new BusinessException("Ticket já está em atendimento");

        var email = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
        var atendente = atendenteRepository.findByUserId(user.getId())
            .orElseThrow(() -> new BusinessException("Atendente não encontrado para este usuário"));

        ticket.setAtendente(atendente);
        ticket.setStatus(StatusTicket.EM_ATENDIMENTO);
        ticket.setAssumidoAt(LocalDateTime.now());
        ticket.getConversa().setAtendente(atendente);
        ticket.getConversa().setStatus(StatusConversa.EM_ATENDIMENTO);

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public void fecharTicket(Long ticketId) {
        var ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> NotFoundException.of("Ticket", ticketId));
        ticket.setStatus(StatusTicket.FECHADO);
        ticket.setFechadoAt(LocalDateTime.now());
        ticket.getConversa().setStatus(StatusConversa.ENCERRADA);
        ticket.getConversa().setEncerradaAt(LocalDateTime.now());
        ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listarMeusTickets() {
        var email = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByEmail(email).orElseThrow();
        var atendenteOpt = atendenteRepository.findByUserId(user.getId());
        if (atendenteOpt.isEmpty()) return List.of();
        return ticketRepository
            .findByAtendenteIdAndStatus(atendenteOpt.get().getId(), StatusTicket.EM_ATENDIMENTO)
            .stream().map(TicketResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listarFila(Long setorId) {
        if (setorId != null) {
            return ticketRepository
                .findBySetorIdAndStatusOrderByCreatedAtAsc(setorId, StatusTicket.ABERTO)
                .stream().map(TicketResponse::from).toList();
        }
        return ticketRepository.findAll().stream()
            .filter(t -> t.getStatus() == StatusTicket.ABERTO)
            .map(TicketResponse::from).toList();
    }
}
