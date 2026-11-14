package inkatech.detective.data;

import java.time.LocalDate;
import java.util.Objects;

public record PuntoTemporal(LocalDate fecha, double valor, boolean parcial) {

    public PuntoTemporal {
        Objects.requireNonNull(fecha, "La fecha es obligatoria");

        if (Double.isInfinite(valor)) {
            throw new IllegalArgumentException("El valor no puede ser infinito");
        }
    }

    public PuntoTemporal(LocalDate fecha, double valor) { //puntos completos
        this(fecha, valor, false);
    }

    public boolean esFaltante() { 
        return Double.isNaN(valor);
    }
}