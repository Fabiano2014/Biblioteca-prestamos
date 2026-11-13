-- SOLO PARA LA DEMO. Las tablas reales las definen los equipos A y C.
CREATE TABLE IF NOT EXISTS libro (
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    titulo TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS ejemplar (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo   TEXT NOT NULL UNIQUE,
    libro_id INTEGER NOT NULL REFERENCES libro(id),
    estado   TEXT NOT NULL DEFAULT 'DISPONIBLE'
);
CREATE TABLE IF NOT EXISTS usuario_sistema (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario TEXT NOT NULL UNIQUE
);
INSERT INTO usuario_sistema (usuario) VALUES ('bibliotecario1');
INSERT INTO libro (titulo) VALUES ('Patrones de Diseño'), ('Clean Code'), ('Java Efectivo');
INSERT INTO ejemplar (codigo, libro_id) VALUES
    ('EJ-001', 1), ('EJ-002', 1), ('EJ-003', 2),
    ('EJ-004', 2), ('EJ-005', 3), ('EJ-006', 3), ('EJ-007', 3);
