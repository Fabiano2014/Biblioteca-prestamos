package edu.biblioteca.model;

import java.time.LocalDate;

/** Fila de solo lectura para las tablas de la vista de préstamos (activos/vencidos). */
public record PrestamoDetalle(
        long prestamoId,
        String codigoLector,
        String nombreLector,
        String codigoEjemplar,
        String tituloLibro,
        LocalDate fechaInicio,
        LocalDate fechaLimite,
        boolean vencido) {
}
