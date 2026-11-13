package edu.biblioteca.service;

import edu.biblioteca.config.Transaccional;
import edu.biblioteca.model.Lector;
import edu.biblioteca.model.TipoLector;
import edu.biblioteca.repository.LectorRepository;
import edu.biblioteca.repository.PrestamoRepository;
import java.util.List;
import java.util.regex.Pattern;

public class LectorService {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final Transaccional tx;
    private final LectorRepository lectores;
    private final PrestamoRepository prestamos;

    public LectorService(Transaccional tx, LectorRepository lectores, PrestamoRepository prestamos) {
        this.tx = tx;
        this.lectores = lectores;
        this.prestamos = prestamos;
    }

    public Lector registrarLector(String codigoUniversitario, String nombre, String correo,
                                  TipoLector tipo) {
        String codigo = texto(codigoUniversitario, "El código universitario es obligatorio");
        String nom = texto(nombre, "El nombre es obligatorio");
        String mail = validarCorreo(correo);
        if (tipo == null) {
            throw new ReglaNegocioException("Debe indicar si es estudiante o docente");
        }
        return tx.ejecutar(() -> {
            if (lectores.buscarPorCodigo(codigo).isPresent()) {
                throw new ReglaNegocioException("Ya existe un lector con el código " + codigo);
            }
            return lectores.guardar(new Lector(codigo, nom, mail, tipo));
        });
    }

    public Lector editarLector(long id, String nombre, String correo, TipoLector tipo) {
        String nom = texto(nombre, "El nombre es obligatorio");
        String mail = validarCorreo(correo);
        if (tipo == null) {
            throw new ReglaNegocioException("Debe indicar si es estudiante o docente");
        }
        return tx.ejecutar(() -> {
            Lector lector = obtener(id);
            lector.actualizarDatos(nom, mail, tipo);
            lectores.actualizar(lector);
            return lector;
        });
    }

    public List<Lector> buscarLector(String texto) {
        return lectores.buscar(texto);
    }

    /** Baja lógica. No se permite si el lector todavía tiene libros sin devolver. */
    public void desactivarLector(long id) {
        tx.ejecutarSinResultado(() -> {
            Lector lector = obtener(id);
            if (!lector.isActivo()) {
                return;
            }
            if (prestamos.contarActivosPorLector(id) > 0) {
                throw new ReglaNegocioException(
                        "No se puede desactivar: el lector tiene préstamos activos");
            }
            lector.desactivar();
            lectores.actualizar(lector);
        });
    }

    private Lector obtener(long id) {
        return lectores.buscarPorId(id)
                .orElseThrow(() -> new ReglaNegocioException("No existe el lector solicitado"));
    }

    private static String texto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException(mensaje);
        }
        return valor.trim();
    }

    private static String validarCorreo(String correo) {
        String c = texto(correo, "El correo es obligatorio");
        if (!CORREO.matcher(c).matches()) {
            throw new ReglaNegocioException("El correo no tiene un formato válido");
        }
        return c;
    }
}
