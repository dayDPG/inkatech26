package inkatech.detective.domain;

import java.time.LocalDate;
import java.util.Objects;

public record PuntoTemporal(LocalDate fecha, double valor) {

    public PuntoTemporal {
        Objects.requireNonNull(fecha, "La fecha es obligatoria");

        if (Double.isInfinite(valor)) {
            throw new IllegalArgumentException("El valor no puede ser infinito");
        }
    }
}
