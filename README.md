# Biblioteca Universitaria

Aplicación de escritorio en **Java 25 + Swing + SQLite** para administrar el catálogo, los préstamos y el mantenimiento de una biblioteca universitaria. Proyecto grupal del curso de Diseño de Patrones (UTP).

Arquitectura **MVC + Service + Repository**, con los patrones **State**, **Facade**, **Proxy** y **Observer**.

## Quién usa la aplicación

Inician sesión el **administrador** y el **bibliotecario**. Los estudiantes y docentes son lectores registrados y no acceden al programa.

## Funcionalidades

- Libros y ejemplares físicos (copias con código único y estado).
- Mantenimiento de ejemplares con historial de motivo e ingreso/habilitación.
- Lectores, préstamos y devoluciones.
- Cuentas del personal con roles.
- Dashboard: total de libros, ejemplares disponibles, préstamos activos y vencidos.

**Reglas principales:** 5 préstamos simultáneos por lector, 7 días calendario, los atrasos bloquean nuevos préstamos. Sin reservas, renovaciones ni multas. Libros y lectores se desactivan lógicamente, sin borrar su historial.

## Estado del proyecto

| Módulo | Responsable | Estado |
|---|---|---|
| A · Catálogo / State (Libro, Ejemplar, mantenimiento) | A | Listo, con pruebas |
| B · Préstamos (Lector, Prestamo, Facade, transacciones) | B | Pendiente |
| C · Seguridad (login, roles, Proxy, cuentas) | C | Pendiente |
| D · Presentación (Swing, controladores, Dashboard, Observer) | D | Pendiente |

## Requisitos

- JDK 25
- Maven 3.9 o superior
- NetBeans (u otro IDE con soporte Maven)

## Cómo empezar

```bash
git clone https://github.com/dhaxty/biblioteca-patrones.git
cd biblioteca-patrones

mvn test          # compila y ejecuta las pruebas
mvn package       # genera el JAR en target/
```

Las pruebas usan SQLite en memoria, así que no hace falta crear ninguna base de datos.

## Estructura

```
src/main/java/edu/biblioteca/
├─ model/            Entidades del dominio
│  └─ estado/        Patrón State del ejemplar
├─ view/             Pantallas Swing
├─ controller/       Controladores MVC
├─ service/          Reglas de negocio y Facade
├─ repository/       Interfaces de repositorio
│  └─ jdbc/          Implementaciones JDBC
├─ security/         Login, permisos y Proxy
├─ event/            Observer
└─ config/           Conexión SQLite y transacciones
src/main/resources/db/   Scripts SQL por módulo
src/test/java/           Pruebas JUnit 5
docs/                    Documentación técnica
```

## Ciclo de vida de un ejemplar (State)

```
Disponible ──prestar──▶ Prestado ──devolver──▶ Disponible
    │                      │
    │                      └─devolverConDanio─▶ En mantenimiento
    └─enviarAMantenimiento─▶ En mantenimiento ──habilitar──▶ Disponible
```

## Documentación

- [`AGENTS.md`](AGENTS.md): arquitectura, reglas, contratos entre módulos y convenciones (para el equipo y para agentes de IA).
- [`docs/PARTE_A.md`](docs/PARTE_A.md): detalle del módulo A y cómo integrarlo.

## Trabajo en equipo

`main` se mantiene estable. Cada tarea sale de un issue, se desarrolla en una rama `feature/modulo-tarea` con commits pequeños y se integra con Pull Request revisado y con las pruebas pasando.

No subir archivos `.db`, la carpeta `target/` ni credenciales.
