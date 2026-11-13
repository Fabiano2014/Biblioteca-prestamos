package edu.biblioteca.repository.jdbc;

import edu.biblioteca.config.TransaccionManager;
import edu.biblioteca.model.Lector;
import edu.biblioteca.model.TipoLector;
import edu.biblioteca.repository.LectorRepository;
import edu.biblioteca.repository.RepositorioException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LectorRepositoryJdbc implements LectorRepository {

    private static final String COLUMNAS = "id, codigo_universitario, nombre, correo, tipo, activo";

    private final TransaccionManager tx;

    public LectorRepositoryJdbc(TransaccionManager tx) {
        this.tx = tx;
    }

    @Override
    public Lector guardar(Lector lector) {
        return tx.conConexion(c -> {
            String sql = "INSERT INTO lector (codigo_universitario, nombre, correo, tipo, activo) "
                    + "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, lector.getCodigoUniversitario());
                ps.setString(2, lector.getNombre());
                ps.setString(3, lector.getCorreo());
                ps.setString(4, lector.getTipo().name());
                ps.setInt(5, lector.isActivo() ? 1 : 0);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new RepositorioException("No se obtuvo el id del lector insertado");
                    }
                    lector.asignarId(keys.getLong(1));
                }
            }
            return lector;
        });
    }

    @Override
    public void actualizar(Lector lector) {
        tx.conConexion(c -> {
            String sql = "UPDATE lector SET nombre = ?, correo = ?, tipo = ?, activo = ? WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, lector.getNombre());
                ps.setString(2, lector.getCorreo());
                ps.setString(3, lector.getTipo().name());
                ps.setInt(4, lector.isActivo() ? 1 : 0);
                ps.setLong(5, lector.getId());
                if (ps.executeUpdate() == 0) {
                    throw new RepositorioException("No existe el lector con id " + lector.getId());
                }
            }
            return null;
        });
    }

    @Override
    public Optional<Lector> buscarPorId(long id) {
        return tx.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM lector WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Optional<Lector> buscarPorCodigo(String codigoUniversitario) {
        return tx.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM lector WHERE codigo_universitario = ?")) {
                ps.setString(1, codigoUniversitario);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Lector> buscar(String texto) {
        String patron = "%" + (texto == null ? "" : texto.trim().toLowerCase()) + "%";
        return tx.conConexion(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM lector "
                    + "WHERE LOWER(codigo_universitario) LIKE ? OR LOWER(nombre) LIKE ? "
                    + "OR LOWER(correo) LIKE ? ORDER BY nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, patron);
                ps.setString(2, patron);
                ps.setString(3, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Lector> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    private static Lector mapear(ResultSet rs) throws SQLException {
        return Lector.reconstruir(
                rs.getLong("id"),
                rs.getString("codigo_universitario"),
                rs.getString("nombre"),
                rs.getString("correo"),
                TipoLector.valueOf(rs.getString("tipo")),
                rs.getInt("activo") == 1);
    }
}
