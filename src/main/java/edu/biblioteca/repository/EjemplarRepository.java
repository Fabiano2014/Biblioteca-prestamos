package edu.biblioteca.repository;

import edu.biblioteca.model.Ejemplar;
import java.util.Optional;

/** STUB del equipo A: solo los métodos que usa el módulo de préstamos. */
public interface EjemplarRepository {
    Optional<Ejemplar> buscarPorCodigo(String codigo);
    void actualizar(Ejemplar ejemplar);
}
