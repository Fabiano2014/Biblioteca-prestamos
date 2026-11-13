package edu.biblioteca.config;

import java.util.function.Supplier;

/**
 * Ejecuta trabajo dentro de una transacción. Es reentrante: si ya hay una transacción
 * abierta en el hilo, el trabajo se une a ella (un solo COMMIT al final de la externa).
 */
public interface Transaccional {
    <R> R ejecutar(Supplier<R> trabajo);
    void ejecutarSinResultado(Runnable trabajo);
}
