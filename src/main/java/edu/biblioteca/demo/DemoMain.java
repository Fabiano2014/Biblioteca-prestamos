package edu.biblioteca.demo;

import edu.biblioteca.config.TransaccionManager;
import edu.biblioteca.event.PublicadorEventos;
import edu.biblioteca.model.Ejemplar;
import edu.biblioteca.model.Lector;
import edu.biblioteca.model.PrestamoDetalle;
import edu.biblioteca.model.TipoLector;
import edu.biblioteca.repository.EjemplarRepository;
import edu.biblioteca.repository.jdbc.LectorRepositoryJdbc;
import edu.biblioteca.repository.jdbc.PrestamoRepositoryJdbc;
import edu.biblioteca.service.BibliotecaFacade;
import edu.biblioteca.service.BibliotecaOperaciones;
import edu.biblioteca.service.LectorService;
import edu.biblioteca.service.MantenimientoService;
import edu.biblioteca.service.PrestamoService;
import edu.biblioteca.service.ReglaNegocioException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.time.Clock;
import java.util.Optional;

/**
 * Demo por consola del módulo de Préstamos. Crea una base SQLite nueva (demo-biblioteca.db),
 * carga datos de ejemplo y recorre los casos principales. NO forma parte de la entrega final:
 * el Main real lo integra el equipo D.
 */
public class DemoMain {

    public static void main(String[] args) throws Exception {
        Path archivo = Path.of("demo-biblioteca.db");
        Files.deleteIfExists(archivo);
        TransaccionManager tx = new TransaccionManager("jdbc:sqlite:" + archivo);

        ejecutarScript(tx, "/sql/schema_demo_otros_equipos.sql");
        ejecutarScript(tx, "/sql/schema_prestamos.sql");

        // --- cableado (así lo hará D en el Main real) ---
        LectorRepositoryJdbc lectorRepo = new LectorRepositoryJdbc(tx);
        PrestamoRepositoryJdbc prestamoRepo = new PrestamoRepositoryJdbc(tx);
        EjemplarRepository ejemplarRepo = new EjemplarRepositoryDemo(tx);
        MantenimientoService mantenimiento = new MantenimientoServiceDemo(ejemplarRepo);
        PublicadorEventos eventos = tipo -> System.out.println("   [evento] " + tipo);

        PrestamoService prestamoSrv = new PrestamoService(
                tx, lectorRepo, ejemplarRepo, prestamoRepo, Clock.systemDefaultZone());
        LectorService lectorSrv = new LectorService(tx, lectorRepo, prestamoRepo);
        BibliotecaOperaciones biblioteca =
                new BibliotecaFacade(tx, prestamoSrv, mantenimiento, eventos);

        long operadorId = 1; // 'bibliotecario1'

        System.out.println("== Registrar lector ==");
        Lector ana = lectorSrv.registrarLector("U2024001", "Ana Torres", "ana@uni.edu", TipoLector.ESTUDIANTE);
        System.out.println("Lector creado con id " + ana.getId());
        intentar("Código duplicado", () ->
                lectorSrv.registrarLector("U2024001", "Otra", "otra@uni.edu", TipoLector.DOCENTE));

        System.out.println("\n== Prestar 5 ejemplares ==");
        for (int i = 1; i <= 5; i++) {
            String codigo = String.format("EJ-%03d", i);
            biblioteca.prestar("U2024001", codigo, operadorId);
            System.out.println("Prestado " + codigo);
        }
        intentar("Sexto préstamo (máximo 5)", () -> biblioteca.prestar("U2024001", "EJ-006", operadorId));

        System.out.println("\n== Préstamos activos ==");
        for (PrestamoDetalle d : biblioteca.listarActivos()) {
            System.out.printf("%s | %s | %s | límite %s%n",
                    d.codigoEjemplar(), d.tituloLibro(), d.nombreLector(), d.fechaLimite());
        }

        System.out.println("\n== Devolución normal y dañada ==");
        biblioteca.devolver("EJ-001", false, null);
        System.out.println("EJ-001 devuelto");
        biblioteca.devolver("EJ-002", true, "Páginas arrancadas");
        System.out.println("EJ-002 devuelto dañado -> en mantenimiento");
        intentar("Prestar ejemplar en mantenimiento", () -> biblioteca.prestar("U2024001", "EJ-002", operadorId));

        System.out.println("\n== Desactivar lector con préstamos activos ==");
        intentar("Desactivar", () -> lectorSrv.desactivarLector(ana.getId()));

        System.out.println("\nActivos: " + prestamoSrv.contarActivos()
                + " | Vencidos: " + prestamoSrv.contarVencidos());
    }

    private static void intentar(String descripcion, Runnable accion) {
        try {
            accion.run();
            System.out.println("!! " + descripcion + ": debía fallar y no falló");
        } catch (ReglaNegocioException e) {
            System.out.println("Rechazado (" + descripcion + "): " + e.getMessage());
        }
    }

    private static void ejecutarScript(TransaccionManager tx, String recurso) throws IOException {
        String sql;
        try (InputStream in = DemoMain.class.getResourceAsStream(recurso)) {
            if (in == null) {
                throw new IOException("No se encontró el recurso " + recurso);
            }
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        StringBuilder limpio = new StringBuilder();
        for (String linea : sql.split("\n")) {
            if (!linea.trim().startsWith("--")) {
                limpio.append(linea).append('\n');
            }
        }
        tx.ejecutarSinResultado(() -> tx.conConexion(c -> {
            try (Statement st = c.createStatement()) {
                for (String sentencia : limpio.toString().split(";")) {
                    if (!sentencia.isBlank()) {
                        st.execute(sentencia);
                    }
                }
            }
            return null;
        }));
    }

    /** Implementación JDBC mínima del repositorio de A, solo para la demo. */
    static class EjemplarRepositoryDemo implements EjemplarRepository {
        private final TransaccionManager tx;

        EjemplarRepositoryDemo(TransaccionManager tx) { this.tx = tx; }

        @Override
        public Optional<Ejemplar> buscarPorCodigo(String codigo) {
            return tx.conConexion(c -> {
                try (var ps = c.prepareStatement("SELECT id, codigo, estado FROM ejemplar WHERE codigo = ?")) {
                    ps.setString(1, codigo);
                    try (var rs = ps.executeQuery()) {
                        return rs.next()
                                ? Optional.of(new Ejemplar(rs.getLong("id"), rs.getString("codigo"),
                                        Ejemplar.Estado.valueOf(rs.getString("estado"))))
                                : Optional.empty();
                    }
                }
            });
        }

        @Override
        public void actualizar(Ejemplar e) {
            tx.conConexion(c -> {
                try (var ps = c.prepareStatement("UPDATE ejemplar SET estado = ? WHERE id = ?")) {
                    ps.setString(1, e.getEstado().name());
                    ps.setLong(2, e.getId());
                    ps.executeUpdate();
                }
                return null;
            });
        }
    }

    /** Mantenimiento mínimo para la demo (sin historial). Lo real lo hace el equipo A. */
    static class MantenimientoServiceDemo implements MantenimientoService {
        private final EjemplarRepository repo;

        MantenimientoServiceDemo(EjemplarRepository repo) { this.repo = repo; }

        @Override
        public void retirarEjemplar(String codigo, String motivo) {
            Ejemplar e = repo.buscarPorCodigo(codigo).orElseThrow();
            e.enviarAMantenimiento();
            repo.actualizar(e);
        }

        @Override
        public void habilitarEjemplar(String codigo) {
            Ejemplar e = repo.buscarPorCodigo(codigo).orElseThrow();
            e.habilitar();
            repo.actualizar(e);
        }
    }
}
