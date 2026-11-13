package edu.biblioteca.config;

import edu.biblioteca.repository.RepositorioException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

/**
 * Gestiona conexiones SQLite y transacciones. Todos los repositorios JDBC (de cualquier
 * equipo) deben pedir su conexión con {@link #conConexion(SqlFuncion)} para que, dentro de
 * {@link #ejecutar(Supplier)}, compartan la misma transacción.
 */
public final class TransaccionManager implements Transaccional {

    @FunctionalInterface
    public interface SqlFuncion<T, R> {
        R aplicar(T t) throws SQLException;
    }

    private final String url;
    private final ThreadLocal<Connection> actual = new ThreadLocal<>();

    /** @param url por ejemplo {@code jdbc:sqlite:biblioteca.db} */
    public TransaccionManager(String url) {
        this.url = url;
    }

    @Override
    public <R> R ejecutar(Supplier<R> trabajo) {
        if (actual.get() != null) {
            return trabajo.get(); // unirse a la transacción existente
        }
        try (Connection c = abrir()) {
            c.setAutoCommit(false);
            actual.set(c);
            try {
                R resultado = trabajo.get();
                c.commit();
                return resultado;
            } catch (RuntimeException | Error e) {
                try {
                    c.rollback();
                } catch (SQLException ignorada) {
                    e.addSuppressed(ignorada);
                }
                throw e;
            } finally {
                actual.remove();
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al ejecutar la transacción", e);
        }
    }

    @Override
    public void ejecutarSinResultado(Runnable trabajo) {
        ejecutar(() -> {
            trabajo.run();
            return null;
        });
    }

    /** Usa la conexión de la transacción activa, o abre y cierra una propia si no hay. */
    public <R> R conConexion(SqlFuncion<Connection, R> accion) {
        Connection enCurso = actual.get();
        try {
            if (enCurso != null) {
                return accion.aplicar(enCurso);
            }
            try (Connection c = abrir()) {
                return accion.aplicar(c);
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error de acceso a SQLite", e);
        }
    }

    private Connection abrir() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
            s.execute("PRAGMA busy_timeout = 5000");
        }
        return c;
    }
}
