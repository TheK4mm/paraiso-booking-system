# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Hotel Paraíso v2.0 — hotel management system. Spring Boot 4.1 (Framework 7, Security 7, Jackson 3) / Java 17 full-stack app: REST API (`/api/...`, HTTP Basic) + server-rendered Thymeleaf UI (`/`, form login) over one service layer. PostgreSQL with Flyway migrations. Code identifiers, comments, commit messages, and the domain language are all in **Spanish** — follow that convention.

Boot 4 is modularized: integrations need their own module (e.g. `spring-boot-flyway`), slice-test annotations live in per-module `-test` artifacts (`org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`, `...webmvc.test.autoconfigure.AutoConfigureMockMvc`, `...jdbc.test.autoconfigure.AutoConfigureTestDatabase`, `...jpa.test.autoconfigure.TestEntityManager`), Testcontainers is NOT managed by the Boot BOM (imported explicitly in dependencyManagement), and Jackson 3 dropped `write-dates-as-timestamps` (ISO-8601 is the default).

## Commands

```bash
mvn spring-boot:run                # run (http://localhost:8080, profile dev by default)
mvn clean package -DskipTests      # build jar
mvn test                           # unit tests (Mockito)
mvn verify                         # + Testcontainers ITs (skip automatically without Docker)
mvn test -Dtest=ClassName#method   # single test
```

Database: local PostgreSQL, database `hotel_paraiso` must exist; Flyway creates/updates the schema on startup (`ddl-auto=validate` — never let Hibernate manage schema). Dev seed data comes from `db/seed-dev/` (profile dev only — kept OUTSIDE `db/migration` because Flyway scans that classpath location recursively and prod would otherwise apply the seeds too). To reset dev data: drop/recreate the database and restart.

Dev credentials: `admin`/`admin123` (ADMIN), `recepcion`/`recepcion123` (RECEPCIONISTA). Config via `application.yml` + `application-dev.yml`/`application-prod.yml`; secrets through env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REMEMBER_ME_KEY`).

## Architecture

**Package-by-feature** under `com.hotel.paraiso`: each module (`cliente/`, `empleado/`, `habitacion/` [includes TipoHabitacion], `servicio/`, `reserva/`, `facturacion/` [Factura+Pago], `dashboard/`, `security/`) contains its entity, repository (+Specs), Request/Response DTOs, MapStruct mapper, service, REST controller and view controller. Cross-cutting code lives in `common/{audit,crud,exception,mapper,validation,web}`.

### Key patterns

- **Catalog CRUD**: Cliente, Empleado, Servicio, TipoHabitacion, Habitacion extend `common/crud/AbstractCrudService` (hooks: `beforeCreate/beforeUpdate` for uniqueness, `applyRelations` for FK resolution, `toDetalle` for counts). **Reserva, Pago, Factura deliberately do NOT** — they have dedicated services with domain logic.
- **Mapping**: MapStruct with `unmappedTargetPolicy = ERROR` (`common/mapper/MapperCentralConfig`) and `disableBuilder` — every new entity/DTO field must be mapped or explicitly ignored or the build fails. Reserva/Pago/Factura mappers are response-only; their entities are built in services.
- **Search**: `JpaSpecificationExecutor` + per-module `*Specs` classes composed with `Specification.allOf`; sort sanitized via `common/web/SortWhitelist`. REST returns `PageResponse<T>`.
- **N+1 discipline**: `@EntityGraph` (to-one only) on paginated `findAll`; `ReservaService.findDetalle` hydrates collections with 3 constant fetch queries (`findByIdConHabitaciones/ConServicios/ConPagosYFactura` — same persistence context merges them). Collection counts use aggregate queries, only in detail views.
- **Business codes**: `RES-yyyy-nnnnnn`/`FAC-yyyy-nnnnnn` come from PostgreSQL sequences (`nextCodigoSeq()`/`nextNumeroSeq()`) — never `count()+1`.
- **Concurrency**: room availability and payment balance are validated under `PESSIMISTIC_WRITE` locks (`findAllByIdForUpdate`, `findByIdForUpdate`); `@Version` on Reserva/Pago/Factura/Habitacion.
- **State machine** (`ReservaService.validarTransicionEstado`): PENDIENTE→CONFIRMADA→CHECKIN→CHECKOUT; CANCELADA from PENDIENTE/CONFIRMADA; NO_SHOW from CHECKIN. CHECKIN sets rooms OCUPADA; leaving CHECKIN frees them. Invalid transitions → `BusinessException` (422).
- **Factura state is derived**: `FacturaService.recalcularEstadoPorReserva` runs after every payment change (PENDIENTE/PAGADA_PARCIALMENTE/PAGADA). Payment limit = factura total (with IVA) if one exists, else reserva price. Descuento may not exceed subtotal.
- **Errors**: services throw `common/exception` types; `ApiExceptionHandler` (@Order(1), RFC 7807 ProblemDetail) covers REST; `MvcExceptionHandler` (@Order(2)) turns business errors into flash+redirect for views. 404→`ResourceNotFoundException`, 400→`BadRequestException`, 422→`BusinessException`, 409→integrity/optimistic-lock.
- **Auditing**: entities extend `AuditableEntity` (creadoEn/actualizadoEn/creadoPor/actualizadoPor via Spring Data auditing reading the SecurityContext). Business events publish `ActividadEvent` → persisted to `activity_log` AFTER_COMMIT only.
- **Security**: `security/SecurityConfig` — form login + sessions + remember-me for UI (CSRF on), HTTP Basic + 401 entry point + CSRF-exempt for `/api/**`. Roles are the `Rol` enum (ADMIN, RECEPCIONISTA). Route rules gate `/usuarios`, `/actividad`, `/empleados` and catalog writes; `@PreAuthorize("hasRole('ADMIN')")` guards `AbstractCrudService.softDelete` and `FacturaService.anular` as defense in depth. Deletes are soft (`activo` flag / estado CANCELADA/ANULADA).

### Frontend

Thymeleaf, layout-dialect. `layout/app.html` = sidebar+topbar admin shell (nav highlighting uses `currentPath` from `GlobalViewAttributes`); `layout/auth.html` = auth/error card. Per-entity templates (`{modulo}/{lista,form,detalle}.html`) built on typed fragments:
- `fragments/campos.html` — form fields bound with `th:field`/`th:errors` via preprocessing (`*{__${campo}__}`). **Use `th:insert` on the grid wrapper div** (`th:replace` would destroy the `col-md-*` class).
- `fragments/tabla.html` — sortable headers + pagination that preserve the query string (`queryBase`/`queryBaseSort` model attrs).
- `fragments/badges.html` (estado→color+Spanish label), `fragments/estados.html` (empty states).
- Confirmations use the generic modal wired by `data-hp-confirm` on forms (see `static/js/app.js`); flash `success`/`error` render as toasts from `layout/app.html`.
- All assets are local (`static/vendor/`, Inter font in `static/fonts/`) — do not add CDN references. Design tokens in `static/css/app.css` (`--hp-*`).

### Gotchas

- Don't index model Maps by enum objects in SpEL (`map[enumVal]` breaks); key label maps by `name()` strings.
- Enum `@Column` needs explicit `length` matching the DDL CHECK columns.
- Lombok `@Builder.Default` values don't apply outside builders; MapStruct mappers use `disableBuilder`, so defaults like `activo=true` are set via `@Mapping(constant=...)` in `toEntity`.
- Never modify an applied Flyway migration (checksum); add a new `V<n>__*.sql`. Dev-only seeds go in `db/seed-dev/` numbered `V1000+` (never inside `db/migration` — recursive scan).
- View controllers bind `@ModelAttribute("request")` DTOs; on `BindingResult` errors re-render the form with the same model attrs (title, editId, select data via `addFormData`).
