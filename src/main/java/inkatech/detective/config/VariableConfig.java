package inkatech.detective.config;

import java.time.LocalDate;
import java.util.Objects;

public record VariableConfig(
        String id,
        String nombre,
        String definicion,
        String unidad,
        RegionGeografica region,
        String dataset,
        String expectativaElNino,
        String archivo,
        String grupo,
        Resolucion resolucion,
        Periodo periodo,
        String notaDeSigno) {

    public VariableConfig {
        id = requireText(id, "El identificador es obligatorio");
        nombre = requireText(nombre, "El nombre es obligatorio");
        definicion = requireText(definicion, "La definicion es obligatoria");
        unidad = requireText(unidad, "La unidad es obligatoria");
        region = Objects.requireNonNull(region, "La region es obligatoria");
        dataset = requireText(dataset, "El dataset es obligatorio");
        expectativaElNino = requireText(
                expectativaElNino,
                "La expectativa para El Nino es obligatoria");
        archivo = requireText(archivo, "El archivo es obligatorio");
        grupo = requireText(grupo, "El grupo es obligatorio");
        resolucion = Objects.requireNonNull(resolucion, "La resolucion es obligatoria");
        periodo = Objects.requireNonNull(periodo, "El periodo es obligatorio");
        notaDeSigno = Objects.requireNonNullElse(notaDeSigno, "");
    }

    public boolean esDiaria() {
        return resolucion == Resolucion.DIARIA;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    //Resolucion temporal de la serie tal como se descarga de Giovanni.
    public enum Resolucion {
        DIARIA,
        MENSUAL
    }

    //Rango de fechas disponible
    public record Periodo(LocalDate desde, LocalDate hasta) {

        public Periodo {
            Objects.requireNonNull(desde, "La fecha inicial es obligatoria");
            Objects.requireNonNull(hasta, "La fecha final es obligatoria");
            if (desde.isAfter(hasta)) {
                throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
            }
        }
    }
}