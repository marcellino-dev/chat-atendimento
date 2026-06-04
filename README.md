# Chat de Atendimento — Backend

Backend do sistema de chat com filas, múltiplos setores, bot automático e integração WhatsApp.

## Stack
- **Java 21** + **Spring Boot 3.2**
- **MySQL 8** com migrações via **Flyway**
- **WebSocket** (STOMP) para tempo real
- **Redis** para cache de sessões e presença
- **JWT** para autenticação
- **RabbitMQ** (Fase 5 — mensageria)

## Como rodar localmente

```bash
# 1. Subir banco + redis
docker-compose up mysql redis -d

# 2. Rodar a aplicação
./mvnw spring-boot:run

# 3. Swagger UI
open http://localhost:8080/api/swagger-ui.html
```

## Fases do projeto

| Fase | Funcionalidades | Status |
|------|----------------|--------|
| 1    | Login, Atendentes, Setores, CRUD base | 🏗️ Em desenvolvimento |
| 2    | Chat RT (WebSocket), Tickets, Filas | ⏳ Próxima |
| 3    | Bot automático + menu de opções | ⏳ Futura |
| 4    | Integração WhatsApp (Evolution API) | ⏳ Futura |
| 5    | Relatórios, SLA, IA, RabbitMQ | ⏳ Futura |

## Estrutura principal

```
src/main/java/com/empresa/chat/
├── config/          # SecurityConfig, WebSocketConfig, SwaggerConfig
├── domain/
│   ├── model/       # Entidades JPA (User, Atendente, Setor, Ticket…)
│   └── enums/       # StatusTicket, StatusAtendente, CanalOrigen
├── repository/      # JPA Repositories
├── service/         # Regras de negócio por módulo
├── controller/      # Endpoints REST + WebSocket
├── dto/             # Request / Response DTOs
├── websocket/       # Handlers STOMP
└── exception/       # GlobalExceptionHandler
```
