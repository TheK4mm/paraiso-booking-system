# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Hotel Paraíso — hotel reservation management system. Spring Boot 3.2.4 / Java 17 full-stack app exposing both a REST API (`/api/...`) and a server-rendered Thymeleaf UI (`/`) over the same service layer. PostgreSQL database. Code identifiers, comments, commit messages, and the domain language are all in **Spanish** — follow that convention.

## Commands

```bash
mvn clean package -DskipTests      # build
mvn spring-boot:run                # run (http://localhost:8080)
java -jar target/hotel-paraiso-1.0.0.jar   # run the packaged jar
mvn test                           # run tests (none exist yet)
mvn test -Dtest=ClassName#method   # run a single test
```

Database setup: requires local PostgreSQL with database `hotel_paraiso` (credentials in `src/main/resources/application.properties`, default `postgres`/`postgres`). Schema in `db/database.sql`; Hibernate runs with `ddl-auto=update`.

## Architecture

Layered, with **two parallel presentation layers** sharing one service layer:

- `controller/` — REST `@RestController`s, all mapped under `/api/...`. The `/api` prefix is declared in each controller's `@RequestMapping` — there is deliberately **no** `server.servlet.context-path` (it was removed to free `/` for Thymeleaf; do not reintroduce it).
- `controller/view/` — Thymeleaf MVC `@Controller`s at root paths (`/clientes`, `/reservas`, …). Never build entity-specific templates; they feed generic ones (see below).
- `service/` — all business logic and validation. Classes are `@Transactional(readOnly = true)` with `@Transactional` on mutating methods. `open-in-view=false`, so entities must be fully mapped to DTOs inside the service.
- `repository/` — Spring Data JPA.
- `model/` — JPA entities; enums (estados) are nested inside their entity (e.g. `Reserva.EstadoReserva`).

### Generic view pattern (DTO ↔ Map ↔ single template)

The Thymeleaf UI renders every entity through just two templates, `pages/list.html` and `pages/form.html`:

1. Each `DTO.Response` (DTOs are nested `Request`/`Response` static classes) has a `toMap()` returning a `LinkedHashMap` with Spanish keys in stable order — these keys are what views reference.
2. Every service implements `IViewMapService<R>` (`findAllAsMap()` / `findByIdAsMap()`), delegating to the existing `findAll()`/`findById()` and calling `toMap()`.
3. ViewControllers declare table columns and form fields using the static helpers in `controller/view/ViewSupport.java` (`column`, `field`, `select`, `multiselect`, `option`) and always return `pages/list` or `pages/form`. Form field `type` values supported by `fragments/form.html`: text, email, number, date, password, textarea, select, multiselect, checkbox.

When adding an entity, the full chain is: model → repository → DTO (Request/Response + `toMap()`) → service implementing `IViewMapService` → REST controller under `/api/...` → ViewController + navbar link.

### Conventions and gotchas

- **Mapping is manual**: each service has a public `toResponse(Entity)` method. MapStruct is configured in `pom.xml` but **unused** — no `@Mapper` interfaces exist; keep mapping manual for consistency.
- **Error handling**: throw the exceptions in `exception/` from services; `GlobalExceptionHandler` maps them — `ResourceNotFoundException` → 404, `BadRequestException` → 400, `BusinessException` (business-rule violations) → 422, bean validation → 400.
- **Reserva state machine**: PENDIENTE → CONFIRMADA → CHECKIN → CHECKOUT, with CANCELADA reachable from PENDIENTE/CONFIRMADA and NO_SHOW from CHECKIN. Enforced in `ReservaService.validarTransicionEstado()`; invalid transitions throw `BusinessException` (422).
- **Deletes are soft**: entities have an `activo` flag (or, for reservations, state becomes CANCELADA); DELETE endpoints deactivate rather than remove rows.
- **Prices are computed server-side** in `ReservaService` (rooms × nights + services); availability is checked per date range via `ReservaRepository.countReservasActivasParaHabitacion`.
- Lombok is used throughout (`@RequiredArgsConstructor` for DI, `@Slf4j`, `@Builder` on entities/DTOs).
