package edu.biblioteca.service;

import static org.junit.jupiter.api.Assertions.*;

import edu.biblioteca.model.Ejemplar;
import edu.biblioteca.model.Lector;
import edu.biblioteca.model.Prestamo;
import edu.biblioteca.model.TipoLector;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PrestamoServiceTest {

    private Fakes.Lectores lectores;
    private Fakes.Ejemplares ejemplares;
    private Fakes.Prestamos prestamos;
    private Lector ana;

    private PrestamoService servicioEn(LocalDate fecha) {
        Clock reloj = Clock.fixed(fecha.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new PrestamoService(new Fakes.TxDirecta(), lectores, ejemplares, prestamos, reloj);
    }

    @BeforeEach
    void preparar() {
        lectores = new Fakes.Lectores();
        ejemplares = new Fakes.Ejemplares();
        prestamos = new Fakes.Prestamos();
        ana = lectores.guardar(new Lector("U001", "Ana", "ana@uni.edu", TipoLector.ESTUDIANTE));
        for (int i = 1; i <= 7; i++) {
            ejemplares.agregar(i, "EJ-" + i, Ejemplar.Estado.DISPONIBLE);
        }
    }

    @Test
    void prestarMarcaEjemplarComoPrestadoYRegistraElPrestamo() {
        Prestamo p = servicioEn(LocalDate.of(2026, 11, 2)).prestar("U001", "EJ-1", 99);
        assertEquals(Ejemplar.Estado.PRESTADO, ejemplares.datos.get("EJ-1").getEstado());
        assertEquals(LocalDate.of(2026, 11, 9), p.getFechaLimite());
        assertEquals(99, p.getOperadorId());
    }

    @Test
    void sextoPrestamoSimultaneoSeRechaza() {
        PrestamoService s = servicioEn(LocalDate.of(2026, 11, 2));
        for (int i = 1; i <= 5; i++) {
            s.prestar("U001", "EJ-" + i, 1);
        }
        assertThrows(ReglaNegocioException.class, () -> s.prestar("U001", "EJ-6", 1));
        assertEquals(Ejemplar.Estado.DISPONIBLE, ejemplares.datos.get("EJ-6").getEstado());
    }

    @Test
    void lectorConPrestamoVencidoNoRecibeNuevosHastaDevolver() {
        servicioEn(LocalDate.of(2026, 11, 2)).prestar("U001", "EJ-1", 1);
        PrestamoService tarde = servicioEn(LocalDate.of(2026, 11, 20));
        assertThrows(ReglaNegocioException.class, () -> tarde.prestar("U001", "EJ-2", 1));

        tarde.devolver("EJ-1"); // se acepta con atraso, sin multa
        assertDoesNotThrow(() -> tarde.prestar("U001", "EJ-2", 1));
    }

    @Test
    void lectorInactivoNoPuedePedirPrestado() {
        ana.desactivar();
        assertThrows(ReglaNegocioException.class,
                () -> servicioEn(LocalDate.of(2026, 11, 2)).prestar("U001", "EJ-1", 1));
    }

    @Test
    void ejemplarNoDisponibleSeRechazaYNoCreaPrestamo() {
        ejemplares.agregar(10, "EJ-M", Ejemplar.Estado.EN_MANTENIMIENTO);
        assertThrows(ReglaNegocioException.class,
                () -> servicioEn(LocalDate.of(2026, 11, 2)).prestar("U001", "EJ-M", 1));
        assertEquals(0, prestamos.datos.size());
    }

    @Test
    void devolverDejaEjemplarDisponibleYCierraElPrestamo() {
        PrestamoService s = servicioEn(LocalDate.of(2026, 11, 2));
        s.prestar("U001", "EJ-1", 1);
        Prestamo cerrado = s.devolver("EJ-1");
        assertFalse(cerrado.estaActivo());
        assertEquals(Ejemplar.Estado.DISPONIBLE, ejemplares.datos.get("EJ-1").getEstado());
    }

    @Test
    void devolverEjemplarSinPrestamoActivoFalla() {
        assertThrows(ReglaNegocioException.class,
                () -> servicioEn(LocalDate.of(2026, 11, 2)).devolver("EJ-1"));
    }
}
