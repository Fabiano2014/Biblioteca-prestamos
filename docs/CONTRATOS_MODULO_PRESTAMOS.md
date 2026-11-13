# Módulo B · Préstamos — contratos con los demás equipos

## Qué entrega B
| Paquete | Clases |
|---|---|
| model | Lector, TipoLector, Prestamo, PrestamoDetalle |
| repository | LectorRepository, PrestamoRepository, RepositorioException |
| repository.jdbc | LectorRepositoryJdbc, PrestamoRepositoryJdbc |
| config | Transaccional, TransaccionManager |
| service | LectorService, PrestamoService, BibliotecaOperaciones, BibliotecaFacade, ReglaNegocioException |
| resources/sql | schema_prestamos.sql |

## Reglas implementadas
- Máx. 5 préstamos activos por lector; plazo de 7 días calendario (límite = inicio + 7).
- Vencido = sigue activo y hoy > fecha límite. Un lector con algún vencido no recibe nuevos préstamos.
- Se puede devolver con atraso (sin multas); al devolver, el lector queda desbloqueado.
- Préstamo + cambio de estado del ejemplar = una sola transacción. Eventos solo tras COMMIT.
- Devolución dañada: se cierra el préstamo y se llama a `MantenimientoService.retirarEjemplar` en la misma transacción.
- Supuesto de B: no se desactiva un lector que tenga préstamos activos.

## Lo que B necesita de A (Catálogo/State)
1. `Ejemplar`: `getId()`, `getCodigo()`, `prestar()`, `devolver()`. Las transiciones inválidas deben lanzar `IllegalStateException`.
2. `EjemplarRepository`: `Optional<Ejemplar> buscarPorCodigo(String)` y `void actualizar(Ejemplar)`.
3. `MantenimientoService`: `retirarEjemplar(String codigo, String motivo)` y `habilitarEjemplar(String codigo)`.
4. **Todos los repositorios JDBC de A deben obtener la conexión con `TransaccionManager.conConexion(...)`**, como hacen los de B; si abren su propia conexión, no entran en la transacción.
5. Tablas esperadas: `ejemplar(id, codigo, libro_id)` y `libro(id, titulo)` (las usa el JOIN de `PrestamoRepositoryJdbc`).

## Lo que B necesita de C (Seguridad)
- Tabla `usuario_sistema(id)`. B solo guarda el `operador_id`; el controlador toma el id de la sesión actual y lo pasa a `prestar(...)`.

## Lo que B necesita de D (Presentación / Observer)
- `PublicadorEventos.publicar(TipoEvento)` y el enum `TipoEvento` (B usa PRESTAMO_REGISTRADO, DEVOLUCION_REGISTRADA, MANTENIMIENTO_ACTUALIZADO).
- Los controladores deben capturar `ReglaNegocioException` y mostrar `getMessage()` al usuario.
- Para el Dashboard: `PrestamoService.contarActivos()` y `contarVencidos()`.

## Cableado (Main / config, lo hace D al integrar)
```java
var tx = new TransaccionManager("jdbc:sqlite:biblioteca.db");
var lectorRepo   = new LectorRepositoryJdbc(tx);
var prestamoRepo = new PrestamoRepositoryJdbc(tx);
var prestamoSrv  = new PrestamoService(tx, lectorRepo, ejemplarRepo, prestamoRepo, Clock.systemDefaultZone());
var lectorSrv    = new LectorService(tx, lectorRepo, prestamoRepo);
BibliotecaOperaciones facade = new BibliotecaFacade(tx, prestamoSrv, mantenimientoSrv, publicador);
```

## Stubs
Las clases marcadas STUB en `src/main/java` (Ejemplar, EjemplarRepository, MantenimientoService, PublicadorEventos, TipoEvento) son versiones mínimas de lo que B espera de A y D, solo para compilar y probar.
**Al integrar, borrarlas**: se reemplazan por las clases reales de cada equipo.
