package edu.biblioteca.repository;

import edu.biblioteca.model.Lector;
import java.util.List;
import java.util.Optional;

public interface LectorRepository {
    /** INSERT; asigna el id generado al lector y lo devuelve. */
    Lector guardar(Lector lector);
    void actualizar(Lector lector);
    Optional<Lector> buscarPorId(long id);
    Optional<Lector> buscarPorCodigo(String codigoUniversitario);
    /** Busca por código, nombre o correo (contiene, sin distinguir mayúsculas). Incluye inactivos. */
    List<Lector> buscar(String texto);
}
