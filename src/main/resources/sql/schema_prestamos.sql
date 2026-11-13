-- Tablas del módulo B (Préstamos). Ejecutar DESPUÉS de las tablas de A (libro, ejemplar)
-- y C (usuario_sistema), o con foreign_keys desactivado durante el bootstrap.

CREATE TABLE IF NOT EXISTS lector (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo_universitario TEXT    NOT NULL UNIQUE,
    nombre               TEXT    NOT NULL,
    correo               TEXT    NOT NULL,
    tipo                 TEXT    NOT NULL CHECK (tipo IN ('ESTUDIANTE', 'DOCENTE')),
    activo               INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0, 1))
);

CREATE TABLE IF NOT EXISTS prestamo (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    lector_id        INTEGER NOT NULL REFERENCES lector(id),
    ejemplar_id      INTEGER NOT NULL REFERENCES ejemplar(id),
    operador_id      INTEGER NOT NULL REFERENCES usuario_sistema(id),
    fecha_inicio     TEXT    NOT NULL,   -- ISO yyyy-MM-dd
    fecha_limite     TEXT    NOT NULL,
    fecha_devolucion TEXT                -- NULL = préstamo activo
);

-- Un ejemplar no puede tener dos préstamos activos a la vez (red de seguridad extra al State).
CREATE UNIQUE INDEX IF NOT EXISTS ux_prestamo_ejemplar_activo
    ON prestamo(ejemplar_id) WHERE fecha_devolucion IS NULL;

CREATE INDEX IF NOT EXISTS ix_prestamo_lector_activo
    ON prestamo(lector_id) WHERE fecha_devolucion IS NULL;
