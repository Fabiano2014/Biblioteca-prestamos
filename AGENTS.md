# AGENTS.md · Biblioteca Universitaria

Guía para agentes de IA (Codex, Antigravity, opencode, etc.) y para el equipo. Léela completa antes de tocar código.

## 1. Stack

- **Java 25**, Swing, SQLite vía JDBC (`org.xerial:sqlite-jdbc`).
- **Maven** (`pom.xml` en la raíz), NetBeans como IDE, JUnit 5 para pruebas.
- Se distribuye como aplicación de escritorio para Windows (JAR).
- Paquete base: `edu.biblioteca`. Todo el código, comentarios, mensajes al usuario y nombres de dominio van en **español** (clases, métodos y variables incluidos). Las palabras técnicas estándar (`Repository`, `Service`, `Facade`, `Proxy`, `Observer`) se mantienen en inglés.

## 2. Producto y alcance

- **Usuarios del programa:** solo el administrador y el bibliotecario inician sesión. Estudiantes y docentes son *lectores registrados* y **no** acceden al programa.
- **Funciones:** administrar libros físicos, ejemplares (copias), lectores, préstamos, devoluciones, mantenimiento, cuentas del personal y estadísticas.
- **Fuera de alcance:** reservas, renovaciones, multas, libros digitales, acceso de lectores.

## 3. Reglas de negocio aprobadas (no cambiar sin acuerdo del equipo)

- Máximo **5 préstamos simultáneos** por lector.
- Plazo de préstamo: **7 días calendario**.
- Un lector con préstamos **vencidos** no puede tomar nuevos préstamos.
- Estados del ejemplar: `Disponible`, `Prestado`, `En mantenimiento`.
- Libros y lectores se **desactivan lógicamente**; nunca se borran, para conservar el historial.
- No se puede desactivar un libro que tenga ejemplares prestados.
- No se agregan ejemplares a un libro desactivado.
- El préstamo y el cambio de estado del ejemplar se guardan en **una sola transacción**.

## 4. Arquitectura: MVC + Service + Repository

```
VIEW → CONTROLLER → SERVICE (+ Facade) → REPOSITORY (interfaz) → JDBC → SQLite
                         ↑
                  MODEL (dominio) · EVENT (Observer, solo después del COMMIT)
```

| Capa | Hace | No hace |
|---|---|---|
| `view` | Pinta formularios y tablas Swing. | No consulta SQLite ni decide reglas ni autoriza. |
| `controller` | Recibe clics, valida entradas básicas, invoca servicios/fachada, muestra errores. | No contiene reglas de negocio ni SQL. |
| `service` | Aplica límites, permisos, vencimientos y transacciones; coordina el dominio. | No depende de Swing. |
| `model` | Entidades y su comportamiento. El patrón State vive aquí. | No conoce JDBC ni la UI. |
| `repository` | Interfaces para guardar/consultar. | Las interfaces no mencionan JDBC. |
| `repository.jdbc` | Implementación JDBC sobre SQLite. | No contiene reglas de negocio. |

## 5. Estructura de paquetes y estado actual

```
src/main/java/edu/biblioteca/
├─ Main.java                      (pendiente · D)
├─ model/                         Libro, Ejemplar, RegistroMantenimiento            [A · listo]
│  └─ estado/                     EstadoEjemplar + 3 estados + TipoEstado           [A · listo]
├─ view/                          Frames y paneles Swing                            [D · pendiente]
├─ controller/                    Controladores MVC                                 [D · pendiente]
├─ service/                       CatalogoService, MantenimientoService             [A · listo]
│                                 PrestamoService, BibliotecaFacade, etc.           [B · pendiente]
├─ repository/                    Libro/Ejemplar/Mantenimiento Repository           [A · listo]
│  └─ jdbc/                       Jdbc* de los tres anteriores                      [A · listo]
├─ security/                      Login, permisos y Proxy                           [C · pendiente]
├─ event/                         Observer + eventos                                [D · pendiente]
└─ config/                        Database (conexión SQLite y transacciones)        [A · listo]
src/main/resources/db/            schema-catalogo.sql (+ un schema por módulo)
src/test/java/                    Pruebas JUnit 5
docs/                             UML y decisiones del equipo (PARTE_A.md)
```

## 6. Modelo de dominio

| Clase | Datos esenciales | Métodos |
|---|---|---|
| `Libro` | ISBN, título, autor, editorial, año, categoría, activo | `actualizarDatos()`, `desactivar()` |
| `Ejemplar` | código único, libro, estado | `prestar()`, `devolver()`, `devolverConDanio()`, `enviarAMantenimiento()`, `habilitar()` |
| `RegistroMantenimiento` | ejemplar, motivo, ingreso, habilitación | `finalizar()`, `estaAbierto()` |
| `Lector` | código universitario, nombre, correo, tipo, activo | `actualizarDatos()`, `desactivar()` *(B)* |
| `Prestamo` | lector, ejemplar, operador, inicio, límite, devolución | `estaVencido(fecha)`, `cerrar()` *(B)* |
| `UsuarioSistema` | usuario, hash de contraseña, rol, activo | `tieneRol()`, `estaActivo()` *(C)* |

Relaciones: Libro 1:N Ejemplar; Ejemplar 1:N Préstamo (histórico); Lector 1:N Préstamo; el personal registra préstamos; Ejemplar 1:N RegistroMantenimiento.

## 7. Patrones GoF

| Patrón | Participantes | Estado |
|---|---|---|
| **State** | `EstadoEjemplar` + `EstadoDisponible`, `EstadoPrestado`, `EstadoEnMantenimiento` | Implementado (A) |
| **Facade** | `BibliotecaOperaciones` + `BibliotecaFacade`: coordina préstamos, devoluciones y mantenimiento desde el controller | Pendiente (B) |
| **Proxy** | `GestionCuentas`, `GestionCuentasReal`, `GestionCuentasProxy`: autoriza tareas exclusivas del administrador | Pendiente (C) |
| **Observer** | `ObservadorEvento`, `PublicadorEventos`, `ObservadorDashboard`: actualiza vistas tras operaciones confirmadas | Pendiente (D) |
| **Factory\*** | Creadores de cuentas/perfiles. Validar Factory Method en el UML; no forzarlo si un único tipo con rol basta | A decidir |

### State: transiciones permitidas

| Operación | Desde | Hacia |
|---|---|---|
| `prestar()` | Disponible | Prestado |
| `devolver()` | Prestado | Disponible |
| `devolverConDanio()` | Prestado | En mantenimiento |
| `enviarAMantenimiento()` | Disponible | En mantenimiento |
| `habilitar()` | En mantenimiento | Disponible |

Cualquier otra combinación lanza `TransicionInvalidaException`. Un ejemplar **prestado** no puede enviarse a mantenimiento directamente: primero se devuelve (con daño si corresponde). Los objetos de estado no tienen datos propios y se reutilizan como singletons (`INSTANCIA`). Para persistir se usa `TipoEstado` (se guarda como texto: `DISPONIBLE`, `PRESTADO`, `EN_MANTENIMIENTO`) y `EstadoEjemplar.desde(TipoEstado)` reconstruye el estado.

## 8. Contratos entre módulos

**Base de datos y transacciones** (`config.Database`)
- Una única conexión compartida (app de escritorio). Los repositorios JDBC reciben `Database` por constructor y usan `db.conexion()`.
- Para agrupar operaciones: `db.enTransaccion(() -> { ... })`. Es **reentrante**: si ya hay una transacción abierta, el bloque interno participa en ella; si algo lanza excepción, se hace rollback de todo.
- `db.inicializarEsquema("/db/schema-catalogo.sql", "/db/schema-prestamos.sql", ...)`: cada módulo aporta su propio `schema-*.sql`. El orden importa si hay claves foráneas (por ejemplo, hacia `ejemplar`).
- Para obtener el id de una fila recién insertada se usa `db.ultimoIdInsertado()` (`last_insert_rowid()`), **no** `getGeneratedKeys()` (no es portable entre versiones del driver).
- Las fechas se guardan como texto ISO-8601 (`LocalDate`/`LocalDateTime.toString()`).

**Errores**
- Reglas de negocio incumplidas → `service.ReglaNegocioException` con mensaje listo para mostrar al usuario.
- Fallos de acceso a datos → `repository.RepositorioException` (envuelve `SQLException`).
- Datos inválidos en el modelo → `IllegalArgumentException`; los servicios la convierten a `ReglaNegocioException`.
- Transición de estado inválida → `TransicionInvalidaException`; los servicios la convierten a `ReglaNegocioException`.

**Para B (Préstamos / Facade)**
- Prestar: `ejemplar.prestar()` + `ejemplarRepository.actualizar(ejemplar)` + insertar el préstamo, todo dentro de `db.enTransaccion(...)`.
- Devolución dañada: `ejemplar.devolverConDanio()` + `ejemplarRepository.actualizar(...)` + `mantenimientoService.registrarIngresoPorDanio(id, motivo)`, en la misma transacción.
- Eventos Observer: publicar **solo después** del `COMMIT`.

**Para D (Presentación / Dashboard)**
- Búsqueda: `catalogoService.buscarLibros(texto)` (título, autor, ISBN o código de ejemplar; solo libros activos).
- Dashboard: `libroRepository.contarActivos()` y `ejemplarRepository.contarPorEstado(TipoEstado.X)`.
- Los controladores atrapan `ReglaNegocioException` y muestran `getMessage()` en un diálogo.

**Para C (Seguridad)**
- El Proxy puede envolver `CatalogoService` y `MantenimientoService` si se decide restringir operaciones por rol. El administrador es el único que crea/desactiva cuentas del personal.

## 9. Convenciones de código

- Cero SQL fuera de `repository.jdbc`. Siempre `PreparedStatement` con parámetros `?`; nunca concatenar datos del usuario en el SQL.
- Las vistas y controladores no acceden a repositorios; hablan con servicios o con la fachada.
- Entidades nuevas: constructor para "nuevo" (sin id) y constructor para "reconstruir desde BD" (con id); `asignarId(long)` solo se llama desde el repositorio al insertar.
- Validar en el constructor/métodos del modelo; las reglas que involucran varios objetos o la BD van en el servicio.
- Contraseñas: solo hash (nunca texto plano), en `security`.
- Prefiere cambios pequeños y enfocados al módulo que te corresponde. No cambies firmas públicas de otro módulo sin avisar: acordar las interfaces antes de implementar.
- No agregar dependencias nuevas al `pom.xml` sin acuerdo del equipo.

## 10. Comandos

```bash
mvn test                 # compila y ejecuta todas las pruebas
mvn package              # genera el JAR en target/
mvn -q -DskipTests package
```

Las pruebas de integración usan SQLite **en memoria** (`Database.enMemoria()`): no necesitan ningún archivo `.db`. Toda funcionalidad nueva debe traer pruebas JUnit 5; antes de abrir un Pull Request, `mvn test` debe pasar completo.

## 11. División del equipo

| Responsable | Alcance principal |
|---|---|
| **A · Catálogo/State** | Libro, Ejemplar, mantenimiento, estados y repositorios relacionados. |
| **B · Préstamos** | Lector, Prestamo, servicios, Facade y transacciones. |
| **C · Seguridad** | UsuarioSistema, autenticación, roles, Proxy y alta de cuentas. |
| **D · Presentación** | Swing, controladores MVC, Dashboard, Observer e integración. |

Pantallas Swing obligatorias: Login/Personal, Libros, Lectores, Préstamos, Mantenimiento y Dashboard (total de libros, ejemplares disponibles, préstamos activos y vencidos).

## 12. Flujo de trabajo en GitHub

`main` estable → issue → rama `feature/modulo-tarea` → commits pequeños → Pull Request → revisión → pruebas → merge.

- **No subir** archivos `.db`, la carpeta `target/` ni credenciales.
- Un agente de IA trabaja siempre sobre una rama, nunca directamente sobre `main`.
- Mensajes de commit en español, en imperativo o descriptivos ("Agregar préstamo con transacción").
