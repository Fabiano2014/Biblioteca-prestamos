package edu.biblioteca.service;

import edu.biblioteca.config.Transaccional;
import edu.biblioteca.model.Ejemplar;
import edu.biblioteca.model.Lector;
import edu.biblioteca.model.Prestamo;
import edu.biblioteca.model.PrestamoDetalle;
import edu.biblioteca.repository.EjemplarRepository;
import edu.biblioteca.repository.LectorRepository;
import edu.biblioteca.repository.PrestamoRepository;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;

/** Dobles de prueba en memoria (sin SQLite). */
final class Fakes {
    private Fakes() {}

    static class TxDirecta implements Transaccional {
        @Override public <R> R ejecutar(Supplier<R> t) { return t.get(); }
        @Override public void ejecutarSinResultado(Runnable t) { t.run(); }
    }

    static class Lectores implements LectorRepository {
        final Map<Long, Lector> datos = new LinkedHashMap<>();
        long seq = 0;
        @Override public Lector guardar(Lector l) { l.asignarId(++seq); datos.put(l.getId(), l); return l; }
        @Override public void actualizar(Lector l) { datos.put(l.getId(), l); }
        @Override public Optional<Lector> buscarPorId(long id) { return Optional.ofNullable(datos.get(id)); }
        @Override public Optional<Lector> buscarPorCodigo(String c) {
            return datos.values().stream().filter(l -> l.getCodigoUniversitario().equals(c)).findFirst();
        }
        @Override public List<Lector> buscar(String t) {
            return datos.values().stream().filter(l -> l.getNombre().toLowerCase().contains(t.toLowerCase())).toList();
        }
    }

    static class Ejemplares implements EjemplarRepository {
        final Map<String, Ejemplar> datos = new HashMap<>();
        Ejemplar agregar(long id, String codigo, Ejemplar.Estado e) {
            Ejemplar x = new Ejemplar(id, codigo, e); datos.put(codigo, x); return x;
        }
        @Override public Optional<Ejemplar> buscarPorCodigo(String c) { return Optional.ofNullable(datos.get(c)); }
        @Override public void actualizar(Ejemplar e) { datos.put(e.getCodigo(), e); }
    }

    static class Prestamos implements PrestamoRepository {
        final List<Prestamo> datos = new ArrayList<>();
        long seq = 0;
        @Override public Prestamo guardar(Prestamo p) { p.asignarId(++seq); datos.add(p); return p; }
        @Override public void cerrar(Prestamo p) { /* el objeto ya está en memoria */ }
        @Override public Optional<Prestamo> buscarActivoPorEjemplar(long id) {
            return datos.stream().filter(p -> p.getEjemplarId() == id && p.estaActivo()).findFirst();
        }
        @Override public int contarActivosPorLector(long id) {
            return (int) datos.stream().filter(p -> p.getLectorId() == id && p.estaActivo()).count();
        }
        @Override public boolean existeVencidoPorLector(long id, LocalDate hoy) {
            return datos.stream().anyMatch(p -> p.getLectorId() == id && p.estaVencido(hoy));
        }
        @Override public List<PrestamoDetalle> listarActivos(LocalDate hoy) { return List.of(); }
        @Override public List<PrestamoDetalle> listarVencidos(LocalDate hoy) { return List.of(); }
        @Override public int contarActivos() { return (int) datos.stream().filter(Prestamo::estaActivo).count(); }
        @Override public int contarVencidos(LocalDate hoy) { return (int) datos.stream().filter(p -> p.estaVencido(hoy)).count(); }
    }
}
