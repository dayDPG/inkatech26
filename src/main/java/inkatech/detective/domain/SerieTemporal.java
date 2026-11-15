package inkatech.detective.domain;

import java.util.List;
import java.util.Objects;
import java.time.LocalDate;

public record SerieTemporal(String variableId, String unidad, List<PuntoTemporal> puntos) {

    public SerieTemporal {
        variableId = requireText(variableId, "El identificador de la variable es obligatorio");
        unidad = requireText(unidad, "La unidad es obligatoria");
        puntos = List.copyOf(Objects.requireNonNull(puntos, "La lista de puntos es obligatoria"));

        validateChronologicalOrder(puntos);
    }

    public SerieTemporal filtrar(LocalDate desde, LocalDate hasta) {
        Objects.requireNonNull(desde, "La fecha inicial es obligatoria");
        Objects.requireNonNull(hasta, "La fecha final es obligatoria");
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }
        return new SerieTemporal(variableId, unidad, puntos.stream()
                .filter(point -> !point.fecha().isBefore(desde) && !point.fecha().isAfter(hasta))
                .toList());
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static void validateChronologicalOrder(List<PuntoTemporal> puntos) {
        for (int index = 0; index < puntos.size(); index++) {
            PuntoTemporal current = Objects.requireNonNull(
                    puntos.get(index),
                    "La serie no puede contener puntos nulos");

            if (index > 0 && !current.fecha().isAfter(puntos.get(index - 1).fecha())) {
                throw new IllegalArgumentException(
                        "Los puntos deben estar ordenados por fecha y no pueden repetirse");
            }
        }
    }
}
