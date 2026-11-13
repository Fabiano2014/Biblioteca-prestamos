package edu.biblioteca.service;

import edu.biblioteca.config.Transaccional;
import edu.biblioteca.event.PublicadorEventos;
import edu.biblioteca.event.TipoEvento;
import edu.biblioteca.model.Prestamo;
import edu.biblioteca.model.PrestamoDetalle;
import java.util.List;

/**
 * Coordina PrestamoService y MantenimientoService en una sola transacción y avisa a los
 * observadores únicamente después del COMMIT (si algo falla, no se publica nada).
 */
public class BibliotecaFacade implements BibliotecaOperaciones {

    private final Transaccional tx;
    private final PrestamoService prestamos;
    private final MantenimientoService mantenimiento;
    private final PublicadorEventos eventos;

    public BibliotecaFacade(Transaccional tx, PrestamoService prestamos,
                            MantenimientoService mantenimiento, PublicadorEventos eventos) {
        this.tx = tx;
        this.prestamos = prestamos;
        this.mantenimiento = mantenimiento;
        this.eventos = eventos;
    }

    @Override
    public Prestamo prestar(String codigoLector, String codigoEjemplar, long operadorId) {
        Prestamo prestamo = tx.ejecutar(
                () -> prestamos.prestar(codigoLector, codigoEjemplar, operadorId));
        eventos.publicar(TipoEvento.PRESTAMO_REGISTRADO);
        return prestamo;
    }

    @Override
    public Prestamo devolver(String codigoEjemplar, boolean danado, String motivoDanio) {
        if (danado && (motivoDanio == null || motivoDanio.isBlank())) {
            throw new ReglaNegocioException("Debe indicar el motivo del daño");
        }
        Prestamo prestamo = tx.ejecutar(() -> {
            Prestamo cerrado = prestamos.devolver(codigoEjemplar);
            if (danado) {
                // Prestado → Disponible → En mantenimiento, todo en la misma transacción
                mantenimiento.retirarEjemplar(codigoEjemplar, motivoDanio.trim());
            }
            return cerrado;
        });
        eventos.publicar(TipoEvento.DEVOLUCION_REGISTRADA);
        if (danado) {
            eventos.publicar(TipoEvento.MANTENIMIENTO_ACTUALIZADO);
        }
        return prestamo;
    }

    @Override
    public void retirarEjemplarAMantenimiento(String codigoEjemplar, String motivo) {
        tx.ejecutarSinResultado(() -> mantenimiento.retirarEjemplar(codigoEjemplar, motivo));
        eventos.publicar(TipoEvento.MANTENIMIENTO_ACTUALIZADO);
    }

    @Override
    public void habilitarEjemplar(String codigoEjemplar) {
        tx.ejecutarSinResultado(() -> mantenimiento.habilitarEjemplar(codigoEjemplar));
        eventos.publicar(TipoEvento.MANTENIMIENTO_ACTUALIZADO);
    }

    @Override
    public List<PrestamoDetalle> listarActivos() {
        return prestamos.listarActivos();
    }

    @Override
    public List<PrestamoDetalle> listarVencidos() {
        return prestamos.listarVencidos();
    }
}
