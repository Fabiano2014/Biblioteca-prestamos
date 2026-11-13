package edu.biblioteca.event;

/** STUB del equipo D (Observer). Se invoca solo después del COMMIT. */
public interface PublicadorEventos {
    void publicar(TipoEvento tipo);
}
