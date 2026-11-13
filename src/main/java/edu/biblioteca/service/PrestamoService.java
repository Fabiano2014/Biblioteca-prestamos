package edu.biblioteca.service;

import edu.biblioteca.config.Transaccional;
import edu.biblioteca.model.Ejemplar;
import edu.biblioteca.model.Lector;
import edu.biblioteca.model.Prestamo;
import edu.biblioteca.model.PrestamoDetalle;
import edu.biblioteca.repository.EjemplarRepository;
import edu.biblioteca.repository.LectorRepository;
import edu.biblioteca.repository.PrestamoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Reglas de préstamo y devolución. Cada operación es atómica: el cambio de estado del
 * ejemplar y el registro del préstamo se guardan en la misma transacción SQLite.
 */
public class PrestamoService {

    public static final int MAX_PRESTAMOS_SIMULTANEOS = 5;

    private final Transaccional tx;
    private final LectorRepository lectores;
    private final EjemplarRepository ejemplares;
    private final PrestamoRepository prestamos;
    private final Clock reloj;

    public PrestamoService(Transaccional tx, LectorRepository lectores,
                           EjemplarRepository ejemplares, PrestamoRepository prestamos,
                           Clock reloj) {
        this.tx = tx;
        this.lectores = lectores;
        this.ejemplares = ejemplares;
        this.prestamos = prestamos;
        this.reloj = reloj;
    }

    public Prestamo prestar(String codigoLector, String codigoEjemplar, long operadorId) {
        return tx.ejecutar(() -> {
            LocalDate hoy = LocalDate.now(reloj);

            Lector lector = lectores.buscarPorCodigo(requerir(codigoLector, "el código del lector"))
                    .orElseThrow(() -> new ReglaNegocioException(
                            "No existe un lector con código " + codigoLector.trim()));
            if (!lector.isActivo()) {
                throw new ReglaNegocioException("El lector está desactivado");
            }
            if (prestamos.existeVencidoPorLector(lector.getId(), hoy)) {
                throw new ReglaNegocioException(
                        "El lector tiene préstamos vencidos y no puede recibir nuevos");
            }
            if (prestamos.contarActivosPorLector(lector.getId()) >= MAX_PRESTAMOS_SIMULTANEOS) {
                throw new ReglaNegocioException("El lector ya tiene "
                        + MAX_PRESTAMOS_SIMULTANEOS + " préstamos activos (máximo permitido)");
            }

            Ejemplar ejemplar = buscarEjemplar(codigoEjemplar);
            try {
                ejemplar.prestar(); // State decide si la transición es válida
            } catch (IllegalStateException e) {
                throw new ReglaNegocioException(
                        "El ejemplar " + ejemplar.getCodigo() + " no se puede prestar: "
                                + e.getMessage(), e);
            }
            ejemplares.actualizar(ejemplar);

            return prestamos.guardar(
                    Prestamo.nuevo(lector.getId(), ejemplar.getId(), operadorId, hoy));
        });
    }

    /**
     * Cierra el préstamo activo del ejemplar y lo deja disponible. Se acepta devolver con
     * atraso (no hay multas); al cerrarse el préstamo vencido, el lector se desbloquea.
     */
    public Prestamo devolver(String codigoEjemplar) {
        return tx.ejecutar(() -> {
            LocalDate hoy = LocalDate.now(reloj);
            Ejemplar ejemplar = buscarEjemplar(codigoEjemplar);
            Prestamo prestamo = prestamos.buscarActivoPorEjemplar(ejemplar.getId())
                    .orElseThrow(() -> new ReglaNegocioException(
                            "El ejemplar " + ejemplar.getCodigo() + " no tiene un préstamo activo"));
            try {
                ejemplar.devolver();
            } catch (IllegalStateException e) {
                throw new ReglaNegocioException(
                        "No se puede devolver el ejemplar " + ejemplar.getCodigo() + ": "
                                + e.getMessage(), e);
            }
            prestamo.cerrar(hoy);
            ejemplares.actualizar(ejemplar);
            prestamos.cerrar(prestamo);
            return prestamo;
        });
    }

    public List<PrestamoDetalle> listarActivos() {
        return prestamos.listarActivos(LocalDate.now(reloj));
    }

    public List<PrestamoDetalle> listarVencidos() {
        return prestamos.listarVencidos(LocalDate.now(reloj));
    }

    /** Para el Dashboard (DashboardService). */
    public int contarActivos() {
        return prestamos.contarActivos();
    }

    /** Para el Dashboard (DashboardService). */
    public int contarVencidos() {
        return prestamos.contarVencidos(LocalDate.now(reloj));
    }

    private Ejemplar buscarEjemplar(String codigo) {
        String c = requerir(codigo, "el código del ejemplar");
        return ejemplares.buscarPorCodigo(c)
                .orElseThrow(() -> new ReglaNegocioException("No existe un ejemplar con código " + c));
    }

    private static String requerir(String valor, String que) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("Debe indicar " + que);
        }
        return valor.trim();
    }
}
