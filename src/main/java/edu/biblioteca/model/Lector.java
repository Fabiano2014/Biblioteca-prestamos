package edu.biblioteca.model;

/** Persona registrada que puede recibir préstamos. No inicia sesión en el programa. */
public class Lector {

    private Long id;
    private final String codigoUniversitario;
    private String nombre;
    private String correo;
    private TipoLector tipo;
    private boolean activo;

    /** Lector nuevo (activo, sin id hasta que el repositorio lo guarde). */
    public Lector(String codigoUniversitario, String nombre, String correo, TipoLector tipo) {
        this(null, codigoUniversitario, nombre, correo, tipo, true);
    }

    private Lector(Long id, String codigoUniversitario, String nombre, String correo,
                   TipoLector tipo, boolean activo) {
        this.id = id;
        this.codigoUniversitario = requerirTexto(codigoUniversitario, "código universitario");
        this.nombre = requerirTexto(nombre, "nombre");
        this.correo = requerirTexto(correo, "correo");
        this.tipo = java.util.Objects.requireNonNull(tipo, "tipo");
        this.activo = activo;
    }

    /** Usado por el repositorio JDBC al leer de la base de datos. */
    public static Lector reconstruir(long id, String codigoUniversitario, String nombre,
                                     String correo, TipoLector tipo, boolean activo) {
        return new Lector(id, codigoUniversitario, nombre, correo, tipo, activo);
    }

    public void actualizarDatos(String nombre, String correo, TipoLector tipo) {
        this.nombre = requerirTexto(nombre, "nombre");
        this.correo = requerirTexto(correo, "correo");
        this.tipo = java.util.Objects.requireNonNull(tipo, "tipo");
    }

    /** Baja lógica: el historial de préstamos se conserva. */
    public void desactivar() {
        this.activo = false;
    }

    /** Lo invoca el repositorio una sola vez, tras el INSERT. */
    public void asignarId(long id) {
        if (this.id != null) {
            throw new IllegalStateException("El lector ya tiene id asignado");
        }
        this.id = id;
    }

    private static String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio");
        }
        return valor.trim();
    }

    public Long getId() { return id; }
    public String getCodigoUniversitario() { return codigoUniversitario; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public TipoLector getTipo() { return tipo; }
    public boolean isActivo() { return activo; }
}
