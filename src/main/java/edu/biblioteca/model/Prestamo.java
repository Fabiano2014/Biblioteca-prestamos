package edu.biblioteca.model;

import java.time.LocalDate;

/** Préstamo de un ejemplar a un lector, registrado por un operador del personal. */
public class Prestamo {

    /** Plazo aprobado: 7 días calendario. */
    public static final int DIAS_PRESTAMO = 7;

    private Long id;
    private final long lectorId;
    private final long ejemplarId;
    private final long operadorId;
    private final LocalDate fechaInicio;
    private final LocalDate fechaLimite;
    private LocalDate fechaDevolucion;

    private Prestamo(Long id, long lectorId, long ejemplarId, long operadorId,
                     LocalDate fechaInicio, LocalDate fechaLimite, LocalDate fechaDevolucion) {
        this.id = id;
        this.lectorId = lectorId;
        this.ejemplarId = ejemplarId;
        this.operadorId = operadorId;
        this.fechaInicio = fechaInicio;
        this.fechaLimite = fechaLimite;
        this.fechaDevolucion = fechaDevolucion;
    }

    public static Prestamo nuevo(long lectorId, long ejemplarId, long operadorId, LocalDate inicio) {
        return new Prestamo(null, lectorId, ejemplarId, operadorId,
                inicio, inicio.plusDays(DIAS_PRESTAMO), null);
    }

    public static Prestamo reconstruir(long id, long lectorId, long ejemplarId, long operadorId,
                                       LocalDate inicio, LocalDate limite, LocalDate devolucion) {
        return new Prestamo(id, lectorId, ejemplarId, operadorId, inicio, limite, devolucion);
    }

    public boolean estaActivo() {
        return fechaDevolucion == null;
    }

    /** Vencido = sigue activo y la fecha dada es posterior a la fecha límite. */
    public boolean estaVencido(LocalDate fecha) {
        return estaActivo() && fecha.isAfter(fechaLimite);
    }

    public void cerrar(LocalDate fecha) {
        if (!estaActivo()) {
            throw new IllegalStateException("El préstamo ya fue cerrado");
        }
        if (fecha.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La devolución no puede ser anterior al inicio");
        }
        this.fechaDevolucion = fecha;
    }

    public void asignarId(long id) {
        if (this.id != null) {
            throw new IllegalStateException("El préstamo ya tiene id asignado");
        }
        this.id = id;
    }

    public Long getId() { return id; }
    public long getLectorId() { return lectorId; }
    public long getEjemplarId() { return ejemplarId; }
    public long getOperadorId() { return operadorId; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaLimite() { return fechaLimite; }
    public LocalDate getFechaDevolucion() { return fechaDevolucion; }
}
