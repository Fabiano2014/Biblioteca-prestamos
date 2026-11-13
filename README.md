# Biblioteca Universitaria · Módulo B (Préstamos)

## Abrir en NetBeans
1. **File → Open Project…** y elige la carpeta `biblioteca-prestamos` (el icono muestra una "m" de Maven).
2. Espera a que NetBeans descargue las dependencias (necesita internet la primera vez).
3. **Run Project (F6)** ejecuta `edu.biblioteca.demo.DemoMain` y muestra la demo en la consola.
4. **Test Project (Alt+F6)** ejecuta las pruebas JUnit 5.

Requiere JDK 21 (Tools → Java Platforms, y Project Properties → Build → Compile si hace falta).

## Qué es de quién
- `model/Lector, Prestamo…`, `repository/`, `config/`, `service/Lector|Prestamo|Biblioteca*` → **B (este módulo)**.
- Marcadas como **STUB** (`Ejemplar`, `EjemplarRepository`, `MantenimientoService`, `PublicadorEventos`, `TipoEvento`):
  versiones mínimas de A y D. Al integrar, **bórralas y usa las reales**.
- `demo/DemoMain.java` y `sql/schema_demo_otros_equipos.sql`: solo para probar B; no se suben al proyecto final.

Contratos con los demás equipos: `docs/CONTRATOS_MODULO_PRESTAMOS.md`.
