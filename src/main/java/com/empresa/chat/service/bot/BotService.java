package com.empresa.chat.service.bot;

import com.empresa.chat.domain.enums.StatusAtendente;
import com.empresa.chat.domain.model.Atendente;
import com.empresa.chat.domain.model.Setor;
import com.empresa.chat.repository.AtendenteRepository;
import com.empresa.chat.repository.SetorRepository;
import com.empresa.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotService {

    private final AtendenteRepository atendenteRepository;
    private final SetorRepository setorRepository;
    private final UserRepository userRepository;

    private final Map<String, Integer> etapaCliente = new ConcurrentHashMap<>();
    private final Map<String, Long> setorIdEscolhido = new ConcurrentHashMap<>();
    private final Map<String, String> ultimaMensagemCliente = new ConcurrentHashMap<>();
    private final Map<String, Boolean> atendimentoFinalizado = new ConcurrentHashMap<>();

    public String processarMensagem(String telefone, String nome, String mensagem) {
        // Se o atendimento já foi finalizado, ignora novas mensagens
        if (atendimentoFinalizado.getOrDefault(telefone, false)) {
            log.info("Atendimento já finalizado para: {}", telefone);
            return null;
        }

        Integer etapa = etapaCliente.getOrDefault(telefone, 0);
        String msgLower = mensagem.toLowerCase().trim();

        // Verifica se é a mesma mensagem repetida (evita duplicação)
        String ultimaMsg = ultimaMensagemCliente.get(telefone);
        if (mensagem.equals(ultimaMsg)) {
            log.info("Mensagem repetida ignorada: {}", mensagem);
            return null;
        }
        ultimaMensagemCliente.put(telefone, mensagem);

        log.info("Bot - Cliente: {} | Etapa: {} | Msg: {}", telefone, etapa, mensagem);

        // Comandos especiais
        if (msgLower.equals("menu") || msgLower.equals("sair") || msgLower.equals("0")) {
            etapaCliente.remove(telefone);
            setorIdEscolhido.remove(telefone);
            ultimaMensagemCliente.remove(telefone);
            atendimentoFinalizado.remove(telefone);
            return menuPrincipal(nome);
        }

        // Etapa 0: Primeira mensagem (boas-vindas)
        if (etapa == 0) {
            etapaCliente.put(telefone, 1);
            return menuPrincipal(nome);
        }

        // Etapa 1: Aguardando escolha do setor (1-5)
        if (etapa == 1) {
            Long setorId = mapearOpcaoSetor(mensagem);

            if (setorId == null) {
                return "❌ Opção inválida! Digite um número de 1 a 5.\n\n" + menuPrincipal(nome);
            }

            Setor setor = setorRepository.findById(setorId).orElse(null);
            if (setor == null || !setor.getAtivo()) {
                return "❌ Setor não encontrado.\n\n" + menuPrincipal(nome);
            }

            setorIdEscolhido.put(telefone, setorId);
            etapaCliente.put(telefone, 2);

            List<Atendente> atendentes = atendenteRepository.findBySetorIdAndStatus(setorId, StatusAtendente.ONLINE);

            if (atendentes.isEmpty()) {
                etapaCliente.put(telefone, 1);
                return "⚠️ Nenhum atendente disponível para *" + setor.getNome() +
                        "* no momento.\n\nDigite outro número ou *MENU* para voltar.";
            }

            return listaAtendentes(atendentes, setor.getNome());
        }

        // Etapa 2: Aguardando escolha do atendente (1-N)
        if (etapa == 2) {
            try {
                int opcao = Integer.parseInt(mensagem.trim());
                Long setorId = setorIdEscolhido.get(telefone);

                if (setorId == null) {
                    log.error("SetorId não encontrado para cliente: {}", telefone);
                    etapaCliente.put(telefone, 1);
                    return menuPrincipal(nome);
                }

                List<Atendente> atendentes = atendenteRepository.findBySetorIdAndStatus(setorId, StatusAtendente.ONLINE);

                if (opcao < 1 || opcao > atendentes.size()) {
                    Setor setor = setorRepository.findById(setorId).orElse(null);
                    return "❌ Opção inválida! Escolha um número entre 1 e " + atendentes.size() + ".\n\n" +
                            listaAtendentes(atendentes, setor != null ? setor.getNome() : "setor");
                }

                Atendente atendente = atendentes.get(opcao - 1);
                Setor setor = setorRepository.findById(setorId).orElse(null);

                String nomeAtendente = userRepository.findById(atendente.getUser().getId())
                        .map(u -> u.getNome())
                        .orElse("Atendente");

                // Marca o atendimento como finalizado para não responder mais
                atendimentoFinalizado.put(telefone, true);

                // Limpa o estado da conversa
                etapaCliente.remove(telefone);
                setorIdEscolhido.remove(telefone);
                ultimaMensagemCliente.remove(telefone);

                return "✅ Você será atendido por *" + nomeAtendente +
                        "* do setor *" + (setor != null ? setor.getNome() : "") + "*.\n\n" +
                        "Aguarde, o atendente irá responder em breve.\n\n" +
                        "Digite *MENU* para reiniciar o atendimento.";

            } catch (NumberFormatException e) {
                Long setorId = setorIdEscolhido.get(telefone);
                if (setorId == null) {
                    etapaCliente.put(telefone, 1);
                    return "❌ Por favor, digite o número do setor desejado (1 a 5).\n\n" + menuPrincipal(nome);
                }
                List<Atendente> atendentes = atendenteRepository.findBySetorIdAndStatus(setorId, StatusAtendente.ONLINE);
                Setor setor = setorRepository.findById(setorId).orElse(null);
                return "❌ Digite apenas o NÚMERO do atendente.\n\n" +
                        listaAtendentes(atendentes, setor != null ? setor.getNome() : "setor");
            }
        }

        return "em_atendimento";
    }

    private String menuPrincipal(String nome) {
        return "👋 Olá, *" + nome + "!* Bem-vindo ao nosso atendimento.\n\n" +
                "Escolha uma opção:\n\n" +
                "1️⃣ - Financeiro\n" +
                "2️⃣ - Suporte Técnico\n" +
                "3️⃣ - Comercial\n" +
                "4️⃣ - Outros\n" +
                "5️⃣ - Certificados\n\n" +
                "📌 Digite o *NÚMERO* da opção desejada.\n" +
                "🔁 Digite *MENU* para voltar.";
    }

    private String listaAtendentes(List<Atendente> atendentes, String nomeSetor) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 *Atendentes disponíveis - ").append(nomeSetor).append("*\n\n");

        for (int i = 0; i < atendentes.size(); i++) {
            Atendente a = atendentes.get(i);
            String nome = userRepository.findById(a.getUser().getId())
                    .map(u -> u.getNome())
                    .orElse("Atendente");
            sb.append(i + 1).append(" - *").append(nome).append("*\n");
        }

        sb.append("\n📌 Digite o *NÚMERO* do atendente desejado.\n");
        sb.append("🔁 Digite *MENU* para voltar.");

        return sb.toString();
    }

    private Long mapearOpcaoSetor(String opcao) {
        String opcaoLimpa = opcao.trim();
        switch (opcaoLimpa) {
            case "1":
                return 1L;
            case "2":
                return 2L;
            case "3":
                return 3L;
            case "4":
                return 4L;
            case "5":
                return 5L;
            default:
                return null;
        }
    }
}