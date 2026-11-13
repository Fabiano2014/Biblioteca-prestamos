package edu.biblioteca.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PrestamoTest {

    private final LocalDate inicio = LocalDate.of(2026, 11, 2);

    @Test
    void limiteEsSieteDiasCalendarioDespuesDelInicio() {
        Prestamo p = Prestamo.nuevo(1, 1, 1, inicio);
        assertEquals(LocalDate.of(2026, 11, 9), p.getFechaLimite());
    }

    @Test
    void noEstaVencidoElDiaLimiteSiSeVenceAlDiaSiguiente() {
        Prestamo p = Prestamo.nuevo(1, 1, 1, inicio);
        assertFalse(p.estaVencido(LocalDate.of(2026, 11, 9)));
        assertTrue(p.estaVencido(LocalDate.of(2026, 11, 10)));
    }

    @Test
    void prestamoCerradoNuncaEstaVencido() {
        Prestamo p = Prestamo.nuevo(1, 1, 1, inicio);
        p.cerrar(LocalDate.of(2026, 11, 20));
        assertFalse(p.estaVencido(LocalDate.of(2026, 12, 1)));
    }

    @Test
    void noSePuedeCerrarDosVeces() {
        Prestamo p = Prestamo.nuevo(1, 1, 1, inicio);
        p.cerrar(inicio);
        assertThrows(IllegalStateException.class, () -> p.cerrar(inicio));
    }
}
