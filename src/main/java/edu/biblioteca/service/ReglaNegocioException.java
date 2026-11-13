package edu.biblioteca.service;

/** Se rompió una regla del negocio. El mensaje está pensado para mostrarse al usuario. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
    public ReglaNegocioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
