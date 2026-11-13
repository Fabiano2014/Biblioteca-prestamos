package edu.biblioteca.repository.jdbc;

import edu.biblioteca.config.TransaccionManager;
import edu.biblioteca.model.Prestamo;
import edu.biblioteca.model.PrestamoDetalle;
import edu.biblioteca.repository.PrestamoRepository;
import edu.biblioteca.repository.RepositorioException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PrestamoRepositoryJdbc implements PrestamoRepository {

    /**
     * Supone las tablas de otros equipos: ejemplar(id, codigo, libro_id) y libro(id, titulo).
     * Ver docs/CONTRATOS_MODULO_PRESTAMOS.md.
     */
    private static final String SELECT_DETALLE =
            "SELECT p.id, l.codigo_universitario, l.nombre, e.codigo AS codigo_ejemplar, "
            + "b.titulo, p.fecha_inicio, p.fecha_limite "
            + "FROM prestamo p "
            + "JOIN lector l ON l.id = p.lector_id "
            + "JOIN ejemplar e ON e.id = p.ejemplar_id "
            + "LEFT JOIN libro b ON b.id = e.libro_id "
            + "WHERE p.fecha_devolucion IS NULL ";

    private final TransaccionManager tx;

    public PrestamoRepositoryJdbc(TransaccionManager tx) {
        this.tx = tx;
    }

    @Override
    public Prestamo guardar(Prestamo prestamo) {
        return tx.conConexion(c -> {
            String sql = "INSERT INTO prestamo (lector_id, ejemplar_id, operador_id, "
                    + "fecha_inicio, fecha_limite) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, prestamo.getLectorId());
                ps.setLong(2, prestamo.getEjemplarId());
                ps.setLong(3, prestamo.getOperadorId());
                ps.setString(4, prestamo.getFechaInicio().toString());
                ps.setString(5, prestamo.getFechaLimite().toString());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new RepositorioException("No se obtuvo el id del préstamo insertado");
                    }
                    prestamo.asignarId(keys.getLong(1));
                }
            }
            return prestamo;
        });
    }

    @Override
    public void cerrar(Prestamo prestamo) {
        tx.conConexion(c -> {
            String sql = "UPDATE prestamo SET fecha_devolucion = ? "
                    + "WHERE id = ? AND fecha_devolucion IS NULL";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, prestamo.getFechaDevolucion().toString());
                ps.setLong(2, prestamo.getId());
                if (ps.executeUpdate() == 0) {
                    throw new RepositorioException(
                            "El préstamo " + prestamo.getId() + " no existe o ya estaba cerrado");
                }
            }
            return null;
        });
    }

    @Override
    public Optional<Prestamo> buscarActivoPorEjemplar(long ejemplarId) {
        return tx.conConexion(c -> {
            String sql = "SELECT id, lector_id, ejemplar_id, operador_id, fecha_inicio, "
                    + "fecha_limite, fecha_devolucion FROM prestamo "
                    + "WHERE ejemplar_id = ? AND fecha_devolucion IS NULL";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, ejemplarId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public int contarActivosPorLector(long lectorId) {
        return tx.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM prestamo WHERE lector_id = ? AND fecha_devolucion IS NULL")) {
                ps.setLong(1, lectorId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    @Override
    public boolean existeVencidoPorLector(long lectorId, LocalDate hoy) {
        return tx.conConexion(c -> {
            String sql = "SELECT 1 FROM prestamo WHERE lector_id = ? "
                    + "AND fecha_devolucion IS NULL AND fecha_limite < ? LIMIT 1";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, lectorId);
                ps.setString(2, hoy.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    @Override
    public List<PrestamoDetalle> listarActivos(LocalDate hoy) {
        return listarDetalle(SELECT_DETALLE + "ORDER BY p.fecha_limite, p.id", hoy, false);
    }

    @Override
    public List<PrestamoDetalle> listarVencidos(LocalDate hoy) {
        return listarDetalle(
                SELECT_DETALLE + "AND p.fecha_limite < ? ORDER BY p.fecha_limite, p.id", hoy, true);
    }

    @Override
    public int contarActivos() {
        return contar("SELECT COUNT(*) FROM prestamo WHERE fecha_devolucion IS NULL", null);
    }

    @Override
    public int contarVencidos(LocalDate hoy) {
        return contar("SELECT COUNT(*) FROM prestamo "
                + "WHERE fecha_devolucion IS NULL AND fecha_limite < ?", hoy);
    }

    private int contar(String sql, LocalDate parametro) {
        return tx.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                if (parametro != null) {
                    ps.setString(1, parametro.toString());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    private List<PrestamoDetalle> listarDetalle(String sql, LocalDate hoy, boolean conFecha) {
        return tx.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                if (conFecha) {
                    ps.setString(1, hoy.toString());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<PrestamoDetalle> lista = new ArrayList<>();
                    while (rs.next()) {
                        LocalDate limite = LocalDate.parse(rs.getString("fecha_limite"));
                        lista.add(new PrestamoDetalle(
                                rs.getLong("id"),
                                rs.getString("codigo_universitario"),
                                rs.getString("nombre"),
                                rs.getString("codigo_ejemplar"),
                                rs.getString("titulo"),
                                LocalDate.parse(rs.getString("fecha_inicio")),
                                limite,
                                hoy.isAfter(limite)));
                    }
                    return lista;
                }
            }
        });
    }

    private static Prestamo mapear(ResultSet rs) throws SQLException {
        String devolucion = rs.getString("fecha_devolucion");
        return Prestamo.reconstruir(
                rs.getLong("id"),
                rs.getLong("lector_id"),
                rs.getLong("ejemplar_id"),
                rs.getLong("operador_id"),
                LocalDate.parse(rs.getString("fecha_inicio")),
                LocalDate.parse(rs.getString("fecha_limite")),
                devolucion == null ? null : LocalDate.parse(devolucion));
    }
}
