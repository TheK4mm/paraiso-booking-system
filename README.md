# Hotel Paraíso — Sistema Full-Stack

Sistema de Gestión de Reservas para el **Hotel Paraíso** — Aplicación **full-stack** desarrollada con **Spring Boot 3**, **Spring Data JPA**, **Thymeleaf** y **PostgreSQL**.

El proyecto expone simultáneamente:
- Una **API REST** completa bajo `/api/...` (lista para integrarse con clientes externos o un frontend SPA).
- Una **interfaz web dinámica** servida con **Thymeleaf** bajo `/` (vistas reutilizables alimentadas por DTOs convertidos a `Map`).

## Descripción

El sistema centraliza la operación del Hotel Paraíso permitiendo:

- **Gestión de clientes** — Registro, búsqueda y actualización de huéspedes
- **Gestión de habitaciones** — Catálogo por tipo, disponibilidad por rango de fechas
- **Gestión de reservas** — Ciclo completo con máquina de estados (Pendiente → Confirmada → Check-in → Check-out)
- **Gestión de pagos** — Registro de pagos parciales con validación contra saldo pendiente
- **Facturación** — Generación automática con cálculo de IVA y descuentos
- **Servicios adicionales** — Spa, alimentación, transporte, lavandería, etc.
- **Control de empleados** — Recepcionistas y personal que gestiona las reservas

### Alcance

| Incluido | No incluido |
|----------|-------------|
| API REST (`/api/...`) — CRUD completo 8 entidades | Autenticación JWT / OAuth2 |
| Frontend Thymeleaf dinámico (`/`) | Reportes gráficos / Dashboard |
| Control de disponibilidad por fechas | Notificaciones email/SMS |
| Máquina de estados para reservas | Integración con pasarelas de pago |
| Cálculo automático de precios | Módulo de inventario |
| Templates genéricos reutilizables | |
| Facturación con IVA y descuentos | |

---

## Tecnologías

| Componente | Tecnología |
|------------|-----------|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.2.4 |
| Persistencia | Spring Data JPA + Hibernate |
| Vista | Thymeleaf + Layout Dialect |
| UI | Bootstrap 5.3 + Bootstrap Icons |
| Base de datos | PostgreSQL 16 |
| Validaciones | Jakarta Validation (`@Valid`) |
| Utilidades | Lombok, MapStruct |
| Build | Maven |
| Pruebas API | Postman |

---

## Arquitectura

El proyecto sigue una **arquitectura en capas** con separación clara de responsabilidades y dos capas de presentación simultáneas (REST + MVC):

```
┌────────────────────────────┐    ┌────────────────────────────┐
│  Cliente externo / Postman │    │  Navegador (Thymeleaf)     │
└─────────────┬──────────────┘    └─────────────┬──────────────┘
              │ JSON                            │ HTML
┌─────────────▼──────────────┐    ┌─────────────▼──────────────┐
│  controller.* (REST)       │    │  controller.view.* (MVC)   │
│  @RestController           │    │  @Controller               │
│  /api/...                  │    │  /clientes, /habitaciones… │
└─────────────┬──────────────┘    └─────────────┬──────────────┘
              │ DTOs                            │ Map<String,Object>
              └────────────┬───────────────────┘
                           │
              ┌────────────▼─────────────┐
              │     SERVICE LAYER        │
              │   findAll()              │
              │   findAllAsMap()  ◄──┐   │
              │   findByIdAsMap() ◄──┘   │ Implementan IViewMapService<R>
              └────────────┬─────────────┘
                           │
              ┌────────────▼─────────────┐
              │   REPOSITORY (JPA)       │
              └────────────┬─────────────┘
                           │
              ┌────────────▼─────────────┐
              │       PostgreSQL         │
              └──────────────────────────┘
```

---

## Modelo de Datos

### Entidades

| Entidad | Descripción |
|---------|-------------|
| `Cliente` | Huéspedes del hotel con datos personales |
| `Empleado` | Personal del hotel que gestiona reservas |
| `TipoHabitacion` | Categorías (Individual, Doble, Suite, Familiar, etc.) |
| `Habitacion` | Habitaciones físicas con número, piso y estado |
| `Servicio` | Servicios adicionales (spa, alimentación, transporte) |
| `Reserva` | Entidad central que conecta clientes, habitaciones y servicios |
| `Pago` | Pagos parciales o totales asociados a una reserva |
| `Factura` | Documento fiscal con subtotal, IVA y descuentos |

### Máquina de Estados — Reserva

```
PENDIENTE ──confirmar──▶ CONFIRMADA ──check-in──▶ CHECKIN ──check-out──▶ CHECKOUT
    │            │                                   │
    │            └──cancelar──┐                    no_show ──▶ NO_SHOW
    └─cancelar─▶ CANCELADA ◄──┘
```

Las transiciones inválidas retornan HTTP 422 con mensaje descriptivo.

---

## Patrón Full-Stack Implementado

Para evitar duplicar HTML por cada entidad, el proyecto implementa un patrón genérico **DTO ↔ Map ↔ Template**:

### 1) DTOs con `toMap()`

Cada `DTO.Response` expone su contenido como `Map<String, Object>` con claves específicas y orden estable (`LinkedHashMap`). Estas claves son las que la vista referencia.

```java
public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", this.id);
    map.put("nombreCompleto", this.nombreCompleto);
    map.put("email", this.email);
    // ...
    return map;
}
```

### 2) Services con `findAllAsMap()` / `findByIdAsMap()`

Todos los servicios implementan la interfaz `IViewMapService<R>`:

```java
public interface IViewMapService<R> {
    List<Map<String, Object>> findAllAsMap();
    Map<String, Object> findByIdAsMap(Long id);
}
```

Además, `TipoHabitacionService` implementa `ICategoryService` (que extiende `IViewMapService`) y expone el alias semántico `getAllCategoriesAsMap()`.

La implementación reutiliza siempre la lógica existente:

```java
@Override
public List<Map<String, Object>> findAllAsMap() {
    return findAll().stream()
            .map(ClienteDTO.Response::toMap)
            .collect(Collectors.toList());
}
```

### 3) ViewControllers: construyen columnas + invocan `findAllAsMap()`

Cada ViewController (paquete `controller.view`) define la estructura de columnas y campos del formulario, llama al servicio y siempre retorna `pages/list` o `pages/form`:

```java
@GetMapping
public String list(Model model) {
    List<Map<String, String>> columns = List.of(
        column("id", "ID"),
        column("nombreCompleto", "Nombre Completo"),
        column("email", "Email")
    );
    model.addAttribute("columns", columns);
    model.addAttribute("rows", service.findAllAsMap());
    model.addAttribute("entityPath", "/clientes");
    return "pages/list";
}
```

### 4) Un único template `pages/list.html`

Renderiza cualquier entidad. Itera columnas y filas (Maps):

```html
<th th:each="col : ${columns}" th:text="${col.label}"></th>
<td th:each="col : ${columns}" th:text="${row.get(col.key)}"></td>
```

### 5) Un único template `pages/form.html`

Sirve tanto para **crear** como para **editar**. El controlador es quien decide pasando `isEdit=true|false` y la URL `action` correspondiente. El fragmento `fragments/form.html` soporta tipos: `text`, `email`, `number`, `date`, `password`, `textarea`, `select`, `multiselect`, `checkbox`.

---

## Endpoints

### API REST — todas bajo `/api/...`

| Recurso | Prefijo | Endpoints estándar |
|---------|---------|--------------------|
| Tipos de Habitación | `/api/tipos-habitacion` | `GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` |
| Habitaciones | `/api/habitaciones` | + `GET /disponibles?entrada&salida` |
| Clientes | `/api/clientes` | + `GET /search?termino` |
| Empleados | `/api/empleados` | CRUD estándar |
| Servicios | `/api/servicios` | + `GET /categoria/{categoria}` |
| Reservas | `/api/reservas` | + `GET /codigo/{codigo}`, `GET /cliente/{id}`, `GET /estado/{estado}`, `PATCH /{id}/estado` |
| Pagos | `/api/pagos` | + `GET /reserva/{reservaId}` |
| Facturas | `/api/facturas` | + `GET /reserva/{reservaId}` |

### Interfaz Web Thymeleaf — rutas en raíz `/`

| Ruta | Descripción |
|------|-------------|
| `GET /` | Página de inicio con catálogo de módulos |
| `GET /<entidad>` | Listado dinámico (tabla genérica) |
| `GET /<entidad>/new` | Formulario de creación |
| `GET /<entidad>/{id}/edit` | Formulario de edición prellenado |
| `POST /<entidad>` | Crear |
| `POST /<entidad>/{id}` | Actualizar |
| `POST /<entidad>/{id}/delete` | Desactivar / cancelar |

Entidades disponibles en la UI:
`/tipos-habitacion`, `/habitaciones`, `/clientes`, `/empleados`, `/servicios`, `/reservas`, `/pagos`, `/facturas`.

### Manejo de errores (API)

| Código | Tipo | Ejemplo |
|--------|------|---------|
| `400` | Validación | Campos obligatorios faltantes, email inválido |
| `400` | Duplicado | Email o documento ya registrado |
| `404` | No encontrado | Recurso con ID inexistente |
| `422` | Regla de negocio | Habitación ya reservada, transición de estado inválida |
| `500` | Error interno | Error inesperado del servidor |

---

## Estructura del Proyecto

```
hotel-paraiso
├── pom.xml
├── db/
│   └── database.sql
└── src/main/
    ├── resources/
    │   ├── application.properties
    │   └── templates/
    │       ├── layout/
    │       │   └── base.html          ← Layout decorador (layout-dialect)
    │       ├── fragments/
    │       │   ├── navbar.html        ← Barra de navegación
    │       │   ├── alerts.html        ← Flash messages
    │       │   ├── table.html         ← Tabla genérica reutilizable
    │       │   └── form.html          ← Formulario genérico (create + edit)
    │       └── pages/
    │           ├── home.html          ← Página de inicio
    │           ├── list.html          ← Vista única de listado
    │           └── form.html          ← Vista única de formulario
    └── java/com/hotel/paraiso/
        ├── HotelParaisoApplication.java
        ├── model/              ← Entidades JPA
        ├── repository/         ← Spring Data JPA
        ├── dto/                ← DTOs Request/Response + toMap()
        ├── service/
        │   ├── IViewMapService.java   ← Interfaz base findAllAsMap/findByIdAsMap
        │   ├── ICategoryService.java  ← Interfaz para TipoHabitacion
        │   ├── ClienteService.java    implements IViewMapService<ClienteDTO.Response>
        │   ├── EmpleadoService.java   …
        │   ├── HabitacionService.java …
        │   ├── ServicioService.java   …
        │   ├── ReservaService.java    …
        │   ├── PagoService.java       …
        │   └── FacturaService.java    …
        ├── controller/         ← API REST (@RestController, /api/...)
        │   ├── ClienteController.java
        │   ├── EmpleadoController.java
        │   ├── HabitacionController.java
        │   ├── TipoHabitacionController.java
        │   ├── ServicioController.java
        │   ├── ReservaController.java
        │   ├── PagoController.java
        │   └── FacturaController.java
        ├── controller/view/    ← MVC Thymeleaf (@Controller, rutas raíz)
        │   ├── HomeViewController.java
        │   ├── ViewSupport.java        ← Helpers de columnas y campos
        │   ├── ClienteViewController.java
        │   ├── EmpleadoViewController.java
        │   ├── HabitacionViewController.java
        │   ├── TipoHabitacionViewController.java
        │   ├── ServicioViewController.java
        │   ├── ReservaViewController.java
        │   ├── PagoViewController.java
        │   └── FacturaViewController.java
        └── exception/          ← Manejo global de errores
            ├── ResourceNotFoundException.java
            ├── BadRequestException.java
            ├── BusinessException.java
            └── GlobalExceptionHandler.java
```

---

## Cómo Ejecutar

1. **Configurar PostgreSQL** — crear la base de datos y aplicar `db/database.sql`.
2. **Ajustar credenciales** en `src/main/resources/application.properties` (`spring.datasource.url`, `username`, `password`).
3. **Compilar** — `mvn clean package -DskipTests`
4. **Ejecutar** — `mvn spring-boot:run` o `java -jar target/hotel-paraiso-1.0.0.jar`
5. **Abrir**:
   - UI web: `http://localhost:8080/`
   - API REST: `http://localhost:8080/api/clientes` (etc.)

---

## Notas sobre cambios respecto a la versión previa

- Se eliminó `server.servlet.context-path=/api`. Los REST controllers ahora declaran el prefijo `/api/...` directamente en `@RequestMapping`, dejando libre la raíz `/` para servir Thymeleaf.
- Se corrigió el `HabitacionController` que antes quedaba en `/api/api/habitaciones` por duplicación de prefijo.
- Se introdujeron las interfaces `IViewMapService<R>` y `ICategoryService`, sin alterar la firma de los métodos `findAll()/findById()/create/update/delete` previos.
- Los DTOs conservan su estructura `Request` / `Response` original; solo se añadió o completó el método `toMap()` en cada Response con claves en español coherentes con los campos.
- La API REST funciona igual que antes — únicamente cambia el prefijo unificado a `/api/...`.

---