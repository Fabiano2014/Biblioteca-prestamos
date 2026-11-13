package edu.biblioteca.repository;

import edu.biblioteca.model.Prestamo;
import edu.biblioteca.model.PrestamoDetalle;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PrestamoRepository {
    Prestamo guardar(Prestamo prestamo);
    /** Persiste la fecha de devolución de un préstamo que sigue activo. */
    void cerrar(Prestamo prestamo);

    Optional<Prestamo> buscarActivoPorEjemplar(long ejemplarId);
    int contarActivosPorLector(long lectorId);
    boolean existeVencidoPorLector(long lectorId, LocalDate hoy);

    List<PrestamoDetalle> listarActivos(LocalDate hoy);
    List<PrestamoDetalle> listarVencidos(LocalDate hoy);
    int contarActivos();
    int contarVencidos(LocalDate hoy);
}
