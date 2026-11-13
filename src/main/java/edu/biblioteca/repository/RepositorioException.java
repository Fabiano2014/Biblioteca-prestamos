package edu.biblioteca.repository;

/** Error técnico de persistencia (SQL, conexión). No es una regla de negocio. */
public class RepositorioException extends RuntimeException {
    public RepositorioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
    public RepositorioException(String mensaje) {
        super(mensaje);
    }
}
