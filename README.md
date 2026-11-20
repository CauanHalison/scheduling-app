# Scheduling App

Plataforma fullstack de agendamento de serviços inspirada no modelo do Cal.com / Calendly. O foco principal do projeto é resolver dois problemas clássicos de sistemas de reserva: **cálculo de disponibilidade entre diferentes fusos horários** e **prevenção estrita de double booking sob concorrência**.

---

## Principais Decisões Técnicas

- **Prevenção de conflitos no nível do banco (PostgreSQL GiST):** além da validação na camada de serviço, as tabelas de disponibilidade, bloqueios e agendamentos utilizam a extensão `btree_gist` com constraints de exclusão (`EXCLUDE USING gist` sobre `tstzrange`). Mesmo que duas requisições cheguem no exato mesmo milissegundo para o mesmo horário, o PostgreSQL garante que apenas uma transação confirme (`201 Created`) e rejeita as demais (`409 Conflict`).
- **Normalização temporal em UTC:** todas as datas e horários são processados e persistidos em UTC (`Instant` / `TIMESTAMP WITH TIME ZONE`). A conversão para o fuso do prestador ou do cliente (`ZoneId` IANA) acontece apenas no cálculo da grade semanal e na camada de apresentação.
- **Notificações transacionais pós-commit com iCalendar (`.ics`):** o disparo de e-mails ocorre de forma assíncrona (Virtual Threads) usando `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` com snapshots imutáveis, anexando convites `.ics` gerados com `ical4j` compatíveis com Google Calendar e Outlook (`METHOD:REQUEST` e `METHOD:CANCEL`).
- **Gestão do convidado sem necessidade de conta:** o cliente pode gerenciar ou cancelar sua reserva diretamente via token único (`cancelToken`) enviado por e-mail, ou solicitar um **Magic Link** temporário (TTL de 15 minutos no Redis, protegido por rate limiting por IP e por e-mail).
- **Cache de disponibilidade no Redis:** os cálculos de dias e horários livres são cacheados no Redis com serialização tipada e invalidados automaticamente quando o prestador altera sua grade, cadastra bloqueios ou recebe/cancela agendamentos.

---

## Stack

### Backend (`/api`)
- **Java 21** + **Spring Boot 3.4** (Web, Data JPA, Validation, Security, Cache, Mail, Actuator)
- **PostgreSQL 16** + **Flyway** (migrações versionadas e constraints `btree_gist`)
- **Redis 7** (cache de slots, blacklist de JWT no logout, tokens de Magic Link e rate limiting)
- **JWT (jjwt)** com rotação de Refresh Token e senhas com **BCrypt** (cost 12)
- **iCal4j** para geração de arquivos `.ics`
- **SpringDoc OpenAPI (Swagger UI)**
- **JUnit 5**, **Mockito** e **Testcontainers** (testes de integração e concorrência real com containers PostgreSQL e Redis)

### Frontend (`/web`)
- **React 19** + **TypeScript** + **Vite**
- **Tailwind CSS v4** + **Lucide React**
- **TanStack React Query v5** (cache, paginação e invalidação de estado assíncrono)
- **React Hook Form** + **Zod** (validação de formulários)
- **React Day Picker** + **date-fns** (calendário público localizado em `pt-BR`)
- **Axios** (interceptors com renovação automática de Access Token via Refresh Token)

---

## Estrutura do Repositório

```text
scheduling-api/
├── api/                    # Aplicação Spring Boot (REST API)
├── web/                    # Aplicação React + TypeScript (SPA)
├── docker-compose.yml      # PostgreSQL, Redis e Mailpit para ambiente local
├── .env.example            # Exemplo de variáveis de ambiente
├── package.json            # Scripts utilitários para orquestrar todo o ambiente
└── Scheduling_app_docs.md  # Documento de especificação e requisitos (SRS)
```

---

## Como Executar Localmente

### Pré-requisitos
- **Java 21**
- **Node.js 22+** e **npm**
- **Docker** e **Docker Compose**

### 1. Configurar variáveis de ambiente
Na raiz do projeto, crie o arquivo `.env` a partir do exemplo:

```bash
cp .env.example .env
```

*(Nota: verifique se a porta `DB_PORT` no seu `.env` corresponde à porta mapeada para o PostgreSQL caso você já tenha uma instância local rodando na `5432`).*

### 2. Preparar o ambiente
O script abaixo sobe os containers do Docker (`postgres`, `redis` e `mailpit`), compila a API e instala as dependências do frontend:

```bash
npm run setup
```

### 3. Iniciar a aplicação completa
Para subir o Docker, o backend e o frontend simultaneamente no mesmo terminal:

```bash
npm run dev
```

Ou, se preferir rodar cada serviço separadamente:

```bash
npm run docker:up
npm run api:dev
npm run web:dev
```

---

## Serviços e Acessos Locais

| Serviço | URL Local | Descrição |
| :--- | :--- | :--- |
| **Frontend (Web)** | `http://localhost:5173` | Interface do prestador e páginas públicas de agendamento |
| **Backend (API)** | `http://localhost:8080/api/v1` | Base URL da API REST |
| **Swagger UI** | `http://localhost:8080/api/v1/docs` | Documentação interativa dos endpoints |
| **Mailpit (Web UI)** | `http://localhost:8025` | Caixa de entrada local para visualizar os e-mails e convites `.ics` |

---

## Executando os Testes e Validações

### Backend (Testes unitários e de integração com Testcontainers)
Com o Docker em execução:

```bash
cd api
./mvnw clean verify -B
```

### Frontend (Linter e Build TypeScript)
```bash
cd web
npm run lint
npm run build
```
