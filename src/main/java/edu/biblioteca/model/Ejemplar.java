package edu.biblioteca.model;

/** STUB del equipo A. Reemplazar por la versión real con patrón State. */
public class Ejemplar {
    public enum Estado { DISPONIBLE, PRESTADO, EN_MANTENIMIENTO }

    private final long id;
    private final String codigo;
    private Estado estado;

    public Ejemplar(long id, String codigo, Estado estado) {
        this.id = id; this.codigo = codigo; this.estado = estado;
    }

    /** Las transiciones inválidas deben lanzar IllegalStateException. */
    public void prestar() { exigir(Estado.DISPONIBLE); estado = Estado.PRESTADO; }
    public void devolver() { exigir(Estado.PRESTADO); estado = Estado.DISPONIBLE; }
    public void enviarAMantenimiento() { exigir(Estado.DISPONIBLE); estado = Estado.EN_MANTENIMIENTO; }
    public void habilitar() { exigir(Estado.EN_MANTENIMIENTO); estado = Estado.DISPONIBLE; }

    private void exigir(Estado esperado) {
        if (estado != esperado) {
            throw new IllegalStateException("el ejemplar está " + estado);
        }
    }

    public long getId() { return id; }
    public String getCodigo() { return codigo; }
    public Estado getEstado() { return estado; }
}
