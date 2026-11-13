package edu.biblioteca.service;

/** STUB del equipo A. Debe usar la misma Transaccional/TransaccionManager que el resto. */
public interface MantenimientoService {
    void retirarEjemplar(String codigoEjemplar, String motivo);
    void habilitarEjemplar(String codigoEjemplar);
}
