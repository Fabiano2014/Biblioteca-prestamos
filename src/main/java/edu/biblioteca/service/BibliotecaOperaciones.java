package edu.biblioteca.service;

import edu.biblioteca.model.Prestamo;
import edu.biblioteca.model.PrestamoDetalle;
import java.util.List;

/** Punto de entrada único para los controladores (patrón Facade). */
public interface BibliotecaOperaciones {

    Prestamo prestar(String codigoLector, String codigoEjemplar, long operadorId);

    /**
     * @param danado       true si el ejemplar vuelve dañado (pasa a En mantenimiento)
     * @param motivoDanio  obligatorio cuando danado == true
     */
    Prestamo devolver(String codigoEjemplar, boolean danado, String motivoDanio);

    void retirarEjemplarAMantenimiento(String codigoEjemplar, String motivo);

    void habilitarEjemplar(String codigoEjemplar);

    List<PrestamoDetalle> listarActivos();

    List<PrestamoDetalle> listarVencidos();
}
